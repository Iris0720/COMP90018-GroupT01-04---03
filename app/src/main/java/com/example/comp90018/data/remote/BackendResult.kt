package com.example.comp90018.data.remote

/** Result returned by the remote backend boundary. */
sealed interface BackendResult<out T> {
    data class Success<T>(val value: T) : BackendResult<T>

    data class Failure(val error: BackendError) : BackendResult<Nothing>
}

data class BackendError(
    val kind: BackendErrorKind,
    val message: String,
    val retryable: Boolean
)

enum class BackendErrorKind {
    SIGNED_OUT,
    INVALID_INPUT,
    NOT_FOUND,
    CONFIGURATION,
    NETWORK,
    REMOTE
}
