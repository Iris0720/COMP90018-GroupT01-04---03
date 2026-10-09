package com.example.comp90018.ui.screens.health

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.comp90018.permissions.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Local fallback until the shared meal repository/recognition branch is integrated. */
@Composable
fun PermissionMealFlow(permissions: PermissionController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { context.getSharedPreferences("permission_meals", 0) }
    var records by remember { mutableStateOf(JSONArray(store.getString("records", "[]"))) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var serving by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun editPhoto(uri: String?) {
        photo = uri
        editing = true
        name = ""
        serving = ""
        calories = ""
        message = if (uri == null) "Manual entry" else "Photo attached. Enter meal details; automatic estimation is not connected."
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingPhoto
        pendingPhoto = null
        if (captured && uri != null) editPhoto(uri)
        else message = "Photo cancelled. Try again, choose a photo or enter manually."
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            message = "No photo selected. You can try again or enter manually."
        } else {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val folder = File(context.filesDir, "permission-meal-photos").apply { mkdirs() }
                        val file = File.createTempFile("selected-", ".image", folder)
                        try {
                            val input = requireNotNull(context.contentResolver.openInputStream(uri))
                            input.use { source -> file.outputStream().use { source.copyTo(it) } }
                            require(file.length() > 0)
                            FileProvider.getUriForFile(context, "${context.packageName}.permissionmeals", file).toString()
                        } catch (error: Exception) {
                            file.delete()
                            throw error
                        }
                    }
                }
                result.onSuccess { editPhoto(it) }
                    .onFailure { message = "Could not read that photo. Try another photo or enter manually." }
                busy = false
            }
        }
    }

    fun takePhoto() {
        attempted = true
        permissions.requestPermission(PermissionType.CAMERA) { status ->
            if (status is PermissionStatus.Granted) {
                try {
                    val folder = File(context.filesDir, "permission-meal-photos").apply { mkdirs() }
                    val file = File.createTempFile("camera-", ".jpg", folder)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.permissionmeals", file)
                    pendingPhoto = uri.toString()
                    camera.launch(uri)
                } catch (error: Exception) {
                    pendingPhoto = null
                    message = "Camera could not open. Choose a photo or enter manually."
                }
            } else {
                message = "Camera is unavailable or not allowed. Choose a photo or enter manually."
            }
        }
    }

    Text("Meal log", style = MaterialTheme.typography.titleLarge)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { takePhoto() }, enabled = !busy && pendingPhoto == null) { Text("Take photo") }
        OutlinedButton(onClick = {
            try {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (error: Exception) {
                message = "Photo picker could not open. Enter the meal manually."
            }
        }, enabled = !busy) { Text("Choose photo") }
        OutlinedButton(onClick = { editPhoto(null) }, enabled = !busy) { Text("Enter manually") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (attempted) {
            val status = permissions.status(PermissionType.CAMERA)
            PermissionStatusCard(PermissionType.CAMERA, status,
                onRetry = { takePhoto() },
                onSettings = { permissions.openSettings(PermissionType.CAMERA, status) })
        }
        message?.let { Text(it) }
        if (editing) {
            Text("Review meal", style = MaterialTheme.typography.titleMedium)
            photo?.let { MealPhotoPreview(it) }
            OutlinedTextField(name, { name = it }, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(serving, { serving = it }, label = { Text("Serving") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(calories, { calories = it }, label = { Text("Calories (optional)") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy, onClick = {
                val value = calories.toDoubleOrNull()
                if (name.isBlank() || serving.isBlank() ||
                    (calories.isNotBlank() && (value == null || !value.isFinite() || value < 0))) {
                    message = "Enter a meal name, serving and a valid non-negative calorie value."
                } else {
                    val updated = JSONArray(records.toString()).put(JSONObject().apply {
                        put("id", UUID.randomUUID().toString())
                        put("name", name.trim()); put("serving", serving.trim())
                        put("calories", value ?: JSONObject.NULL)
                        put("photoUri", photo ?: JSONObject.NULL)
                        put("timestamp", System.currentTimeMillis())
                    })
                    busy = true
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            store.edit().putString("records", updated.toString()).commit()
                        }
                        if (saved) {
                            records = updated; editing = false; photo = null
                            message = "Meal saved on this device."
                        } else message = "Could not save. Your entries are kept; please retry."
                        busy = false
                    }
                }
            }) { Text("Save meal") }
            TextButton(onClick = { editing = false; photo = null; message = "Meal entry cancelled." }, enabled = !busy) { Text("Cancel") }
        }
        for (index in records.length() - 1 downTo 0) {
            val record = records.getJSONObject(index)
            Text("${record.getString("name")} · ${record.getString("serving")}")
        }
    }
}

@Composable
private fun MealPhotoPreview(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                }
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        androidx.compose.foundation.Image(it.asImageBitmap(), "Meal photo", Modifier.fillMaxWidth().height(160.dp))
    }
}

