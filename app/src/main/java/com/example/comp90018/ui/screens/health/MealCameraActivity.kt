package com.example.comp90018.ui.screens.health

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.comp90018.ui.theme.TrailwiseTheme

class MealCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outputUri = intent.getStringExtra(EXTRA_OUTPUT_URI)?.let(Uri::parse)
        if (outputUri == null) {
            finishWithResult(Activity.RESULT_CANCELED)
            return
        }

        setContent {
            TrailwiseTheme {
                MealCameraScreen(
                    outputUri = outputUri,
                    onClose = { finishWithResult(Activity.RESULT_CANCELED) },
                    onCaptured = { finishWithResult(Activity.RESULT_OK) }
                )
            }
        }
    }

    private fun finishWithResult(result: Int) {
        setResult(result)
        finish()
    }

    companion object {
        const val EXTRA_OUTPUT_URI = "meal_output_uri"
    }
}

@Composable
private fun MealCameraScreen(
    outputUri: Uri,
    onClose: () -> Unit,
    onCaptured: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                imageCapture = capture
            } catch (_: Exception) {
                cameraError = "Camera preview could not be started."
            }
        }
        providerFuture.addListener(listener, executor)
        onDispose {
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Surface(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
            color = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(24.dp)
        ) {
            TextButton(onClick = onClose) {
                Text("Close", color = Color.White)
            }
        }

        cameraError?.let { message ->
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(message, color = Color.White)
                Button(onClick = onClose) { Text("Back") }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button
                    capturing = true
                    previewView.display?.rotation?.let { capture.targetRotation = it }
                    val output = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        outputUri,
                        ContentValues()
                    ).build()
                    capture.takePicture(
                        output,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                capturing = false
                                onCaptured()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                capturing = false
                                cameraError = "Photo could not be saved. Please try again."
                            }
                        }
                    )
                },
                enabled = imageCapture != null && !capturing && cameraError == null,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                if (capturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text("●")
                }
            }
        }
    }
}
