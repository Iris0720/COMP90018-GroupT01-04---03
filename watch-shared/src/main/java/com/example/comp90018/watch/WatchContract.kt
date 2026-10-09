package com.example.comp90018.watch

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val WATCH_COMMAND_PATH = "/trailwise/watch/command/v1"
const val PHONE_CAPABILITY = "trailwise_phone_v1"

@Serializable enum class WatchAction { READ_STATE, START, PAUSE, RESUME, FINISH }
@Serializable enum class WatchPhase { IDLE, LIVE, PAUSED, COMPLETE }
@Serializable enum class WatchActivityType { WALKING, RUNNING, HIKING }
@Serializable enum class WatchGoalType { OPEN, DURATION_SECONDS, DISTANCE_METRES }
@Serializable enum class AckStatus { ACCEPTED, DUPLICATE, REJECTED }
@Serializable enum class WatchFailure {
    INVALID_COMMAND, COMMAND_ID_REUSED, SIGNED_OUT, ACCOUNT_CHANGED,
    WRONG_SESSION, INVALID_TRANSITION, NOT_READY, DISCONNECTED, TIMEOUT,
    SAVE_FAILED, REMOTE_ERROR, AMBIGUOUS_PHONE
}

@Serializable
data class WatchStartOptions(
    val activityType: WatchActivityType = WatchActivityType.WALKING,
    val goalType: WatchGoalType = WatchGoalType.OPEN,
    val goalValue: Double? = null
)

@Serializable
data class WatchCommand(
    val commandId: String,
    val action: WatchAction,
    val sessionId: String? = null,
    val options: WatchStartOptions? = null,
    // Opaque account-generation binding, NOT a Supabase token or caller-supplied owner ID.
    val accountToken: String? = null,
    val version: Int = 1
)

@Serializable
data class WatchSessionState(
    val sessionId: String? = null,
    val phase: WatchPhase = WatchPhase.IDLE,
    val activityType: WatchActivityType? = null,
    val durationSeconds: Long = 0,
    val distanceMetres: Double = 0.0,
    val steps: Long? = null,
    val saved: Boolean = false,
    val revision: Long = 0,
    val demo: Boolean = false
)

@Serializable
data class WatchAcknowledgement(
    val commandId: String,
    val status: AckStatus,
    val state: WatchSessionState? = null,
    val accountToken: String? = null,
    val failure: WatchFailure? = null,
    val retryable: Boolean = false,
    val version: Int = 1
) {
    val successful: Boolean get() = status != AckStatus.REJECTED
    companion object {
        fun rejected(command: WatchCommand, reason: WatchFailure, retryable: Boolean = false) =
            WatchAcknowledgement(command.commandId, AckStatus.REJECTED, failure = reason, retryable = retryable)
    }
}

fun interface WatchBridge {
    suspend fun sendWatchCommand(command: WatchCommand): WatchAcknowledgement
}

/** Package 3 supplies the real activity controller; Package 1 supplies the current owner.
 * Called off the UI thread. Implementations must serialize phone/watch mutations together,
 * preserve stable session IDs, and report saved=true only after persistent Finish succeeds.
 */
interface PhoneSessionGateway {
    fun currentOwnerId(): String?
    fun snapshot(): WatchSessionState
    // Trusted owner captured by the dispatcher, never taken from wire input.
    // Check it atomically with session mutation against the host's auth state.
    fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState
}

class WatchCommandException(val reason: WatchFailure, val retryable: Boolean = false) : Exception()

object WatchWire {
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }
    const val MAX_BYTES = 8192
    fun encode(command: WatchCommand): ByteArray = checked(json.encodeToString(command).toByteArray(Charsets.UTF_8))
    fun encode(ack: WatchAcknowledgement): ByteArray = checked(json.encodeToString(ack).toByteArray(Charsets.UTF_8))
    fun command(bytes: ByteArray): WatchCommand = json.decodeFromString(checked(bytes).toString(Charsets.UTF_8))
    fun acknowledgement(bytes: ByteArray): WatchAcknowledgement = json.decodeFromString(checked(bytes).toString(Charsets.UTF_8))
    private fun checked(bytes: ByteArray): ByteArray {
        require(bytes.size in 1..MAX_BYTES) { "Invalid watch payload size" }
        return bytes
    }
}
