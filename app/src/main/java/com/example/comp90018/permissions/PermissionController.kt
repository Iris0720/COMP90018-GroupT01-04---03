package com.example.comp90018.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class PermissionType(val title: String, val purpose: String) {
    LOCATION("Location", "record your route and calculate distance while an activity is running"),
    ACTIVITY_RECOGNITION("Activity recognition", "read step counts during an activity"),
    CAMERA("Camera", "take a photo to estimate meal nutrition"),
    NOTIFICATIONS("Notifications", "show optional activity updates")
}

sealed interface PermissionStatus {
    data class Granted(val detail: String? = null) : PermissionStatus
    data class Denied(val canAskAgain: Boolean) : PermissionStatus
    data class Unavailable(
        val reason: String,
        val settingsTarget: SettingsTarget? = null
    ) : PermissionStatus
}

enum class SettingsTarget { APP, LOCATION }

interface PermissionGateway {
    fun read(permission: PermissionType): PermissionStatus
    fun requestPermission(permission: PermissionType, onResult: (PermissionStatus) -> Unit = {})
}

/** Reads Android permission state and remembers whether the system prompt has been shown. */
class PermissionManager(context: Context) {
    private val appContext = context.applicationContext
    private val activity = context.findActivity()
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(type: PermissionType): PermissionStatus {
        unavailableReason(type)?.let { return it }
        val permissions = permissionsFor(type)
        if (type == PermissionType.NOTIFICATIONS &&
            (permissions.isEmpty() || permissions.all {
                ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
            }) && !NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        ) return PermissionStatus.Denied(canAskAgain = false)
        if (permissions.isEmpty()) return PermissionStatus.Granted("Not required on this Android version")

        val grantedPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
        }
        val locationGranted = type == PermissionType.LOCATION && grantedPermissions.isNotEmpty()
        if (grantedPermissions.size == permissions.size || locationGranted) {
            val detail = if (type == PermissionType.LOCATION && Manifest.permission.ACCESS_FINE_LOCATION !in grantedPermissions) {
                "Approximate location"
            } else null
            return PermissionStatus.Granted(detail)
        }

        val requestedCount = preferences.getInt(requestCountKey(type), 0)
        val rationaleAvailable = activity != null && permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        return PermissionStatus.Denied(canAskAgain = rationaleAvailable || requestedCount <= 1)
    }

    internal fun permissionsFor(type: PermissionType): Array<String> = when (type) {
        PermissionType.LOCATION -> arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        PermissionType.ACTIVITY_RECOGNITION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
        } else emptyArray()
        PermissionType.CAMERA -> arrayOf(Manifest.permission.CAMERA)
        PermissionType.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else emptyArray()
    }

    internal fun markRequestStarted(type: PermissionType) {
        preferences.edit().putInt(requestCountKey(type), preferences.getInt(requestCountKey(type), 0) + 1).apply()
    }

    internal fun recordGranted(type: PermissionType) {
        preferences.edit().remove(requestCountKey(type)).apply()
    }

    internal fun shouldExplain(type: PermissionType): Boolean {
        val activity = activity ?: return false
        return permissionsFor(type).any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    private fun unavailableReason(type: PermissionType): PermissionStatus.Unavailable? = when (type) {
        PermissionType.LOCATION -> {
            val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager?.isLocationEnabled == true
            } else {
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            }
            if (enabled) null else PermissionStatus.Unavailable(
                "Turn on device location to record an activity route.",
                SettingsTarget.LOCATION
            )
        }
        PermissionType.ACTIVITY_RECOGNITION -> {
            val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            if (sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null) {
                PermissionStatus.Unavailable("This device does not provide a step counter.")
            } else null
        }
        PermissionType.NOTIFICATIONS -> null
        PermissionType.CAMERA -> if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            PermissionStatus.Unavailable("This device does not have a camera.")
        } else null
    }

    private fun requestCountKey(type: PermissionType) = "request_count_${type.name}"

    private companion object {
        const val PREFERENCES_NAME = "permission_request_history"
    }
}

/** Compose-facing permission API. Request launchers are registered with the Activity lifecycle. */
class PermissionController internal constructor(
    private val manager: PermissionManager,
    private val statuses: MutableState<Map<PermissionType, PermissionStatus>>,
    private val requestAction: (PermissionType, (PermissionStatus) -> Unit) -> Unit,
    private val settingsAction: (PermissionType, PermissionStatus) -> Unit
) : PermissionGateway {
    override fun read(permission: PermissionType): PermissionStatus = manager.read(permission)

    fun status(permission: PermissionType): PermissionStatus =
        statuses.value[permission] ?: manager.read(permission)

    fun refresh() {
        statuses.value = PermissionType.entries.associateWith(manager::read)
    }

    override fun requestPermission(permission: PermissionType, onResult: (PermissionStatus) -> Unit) {
        requestAction(permission, onResult)
    }

    fun openSettings(permission: PermissionType, status: PermissionStatus) {
        settingsAction(permission, status)
    }
}

@Composable
fun rememberPermissionController(): PermissionController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { PermissionManager(context) }
    val statuses = remember(manager) { mutableStateOf(PermissionType.entries.associateWith(manager::read)) }
    val pending = remember(manager) { PendingRequest<PermissionType, PermissionStatus>() }
    var rationalePermission by remember { mutableStateOf<PermissionType?>(null) }

    fun completePending() {
        val type = pending.type ?: return
        val result = manager.read(type)
        if (result is PermissionStatus.Granted) manager.recordGranted(type)
        statuses.value = statuses.value + (type to result)
        rationalePermission = null
        pending.complete(result)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        completePending()
    }

    fun launchPendingRequest() {
        val type = pending.type ?: return
        manager.markRequestStarted(type)
        launcher.launch(manager.permissionsFor(type))
        rationalePermission = null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, manager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statuses.value = PermissionType.entries.associateWith(manager::read)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val controller = PermissionController(
        manager = manager,
        statuses = statuses,
        requestAction = { type, onResult ->
            val current = manager.read(type)
            statuses.value = statuses.value + (type to current)
            when (current) {
                is PermissionStatus.Granted,
                is PermissionStatus.Unavailable -> onResult(current)
                is PermissionStatus.Denied -> {
                    if (!current.canAskAgain) {
                        onResult(current)
                    } else {
                        val permissions = manager.permissionsFor(type)
                        if (permissions.isEmpty()) {
                            val result = PermissionStatus.Granted("Not required on this Android version")
                            statuses.value = statuses.value + (type to result)
                            onResult(result)
                        } else {
                            if (!pending.begin(type, onResult)) {
                                onResult(PermissionStatus.Unavailable("Another permission request is in progress. Please try again."))
                            } else if (manager.shouldExplain(type)) {
                                rationalePermission = type
                            } else {
                                launchPendingRequest()
                            }
                        }
                    }
                }
            }
        },
        settingsAction = { type, status ->
            val intent = when {
                type == PermissionType.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                status is PermissionStatus.Unavailable && status.settingsTarget == SettingsTarget.LOCATION ->
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    )

    rationalePermission?.let { type ->
        AlertDialog(
            onDismissRequest = {
                completePending()
            },
            title = { Text("Allow ${type.title.lowercase()}?") },
            text = { Text("Trailwise needs this permission to ${type.purpose}. You can continue without it, but this feature may be limited.") },
            confirmButton = {
                TextButton(onClick = { launchPendingRequest() }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    completePending()
                }) { Text("Not now") }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    return controller
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
