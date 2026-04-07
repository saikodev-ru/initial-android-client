package ru.saikodev.initial.ui.components

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.saikodev.initial.ui.auth.AuthViewModel
import ru.saikodev.initial.domain.model.User
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QrScannerScreen(
    viewModel: AuthViewModel,
    loginToken: String? = null,
    linkToken: String? = null,
    onBack: () -> Unit,
    onAuthSuccess: (User) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanResult by viewModel.scanResult.collectAsState()
    var hasCameraPermission by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var processed by remember { mutableStateOf(false) }

    // Check camera permission
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Handle direct login/link tokens
    LaunchedEffect(loginToken, linkToken) {
        if (!processed) {
            processed = true
            when {
                loginToken != null -> viewModel.handleLoginToken(loginToken)
                linkToken != null -> viewModel.handleLinkToken(linkToken)
            }
        }
    }

    // Handle scan result
    LaunchedEffect(scanResult) {
        when (val result = scanResult) {
            is AuthViewModel.ScanResult.LoginApproved -> onAuthSuccess(result.user)
            is AuthViewModel.ScanResult.LinkConsumed -> onAuthSuccess(result.user)
            is AuthViewModel.ScanResult.Error -> { /* Show error toast */ }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Сканирование QR") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                }
            },
            actions = {
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(
                        if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        "Вспышка"
                    )
                }
            }
        )

        // Camera preview
        if (hasCameraPermission) {
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                            processImage(imageProxy, viewModel)
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageAnalysis
                                    )
                                    camera.cameraControl.enableTorch(torchEnabled)
                                } catch (e: Exception) {
                                    Log.e("QrScanner", "Camera bind failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Scan overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Corner markers
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val cornerLen = 40.dp.toPx()
                        val strokeWidth = 4.dp.toPx()
                        val w = size.width
                        val h = size.height

                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(0f, cornerLen),
                            end = androidx.compose.ui.geometry.Offset(0f, 0f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(cornerLen, 0f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(w - cornerLen, 0f),
                            end = androidx.compose.ui.geometry.Offset(w, 0f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(w, 0f),
                            end = androidx.compose.ui.geometry.Offset(w, cornerLen),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(0f, h - cornerLen),
                            end = androidx.compose.ui.geometry.Offset(0f, h),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(0f, h),
                            end = androidx.compose.ui.geometry.Offset(cornerLen, h),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(w - cornerLen, h),
                            end = androidx.compose.ui.geometry.Offset(w, h),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = androidx.compose.ui.geometry.Offset(w, h - cornerLen),
                            end = androidx.compose.ui.geometry.Offset(w, h),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Hint text
                Text(
                    "Наведите камеру на QR-код",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        } else {
            // No permission
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Требуется доступ к камере",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Для сканирования QR-кода необходим доступ к камере",
                        fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        // Request permission (handled by Activity)
                    }) {
                        Text("Предоставить доступ")
                    }
                }
            }
        }
    }
}

private fun processImage(imageProxy: ImageProxy, viewModel: AuthViewModel) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val value = barcode.rawValue ?: continue
                    Log.d("QrScanner", "Found barcode: $value")
                    if (value.contains("qr=") || value.contains("qr_link=") ||
                        (value.length >= 32 && value.matches(Regex("[a-fA-F0-9]+")))
                    ) {
                        viewModel.handleQrScan(value)
                        break
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
