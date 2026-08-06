package com.vjuhbee.beecalc.ui.hives

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.vjuhbee.beecalc.R
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Экран сканирования QR-кода улья (SPEC.md §9, v0.4).
 * Preview через CameraX PreviewView, декодирование кадров — в
 * [QrAnalyzer] через ZXing (офлайн). При считывании QR вызывает
 * [onQrScanned] со строкой `beecalc://hive/<uuid>`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        PermissionPrompt(
            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = onBack
        )
        return
    }

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var scanningEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            scanningEnabled = false
            executor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_scan_title)) },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .heightIn(min = 40.dp)
                    ) {
                        Text(stringResource(R.string.qr_scan_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor, QrAnalyzer { raw ->
                            if (scanningEnabled) {
                                scanningEnabled = false
                                onQrScanned(raw)
                            }
                        })
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
            Text(
                text = stringResource(R.string.qr_scan_hint),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    onGrant: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.qr_scan_permission),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 48.dp)
        )
        Button(
            onClick = onGrant,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.qr_scan_permission_ok))
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.qr_scan_back))
        }
    }
}

/**
 * Анализатор кадров камеры: декодирует QR из [ImageProxy] через ZXing.
 * Срабатывает на каждом кадре до первого успешного распознавания.
 */
class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val hints: Map<DecodeHintType, Any> =
        mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = proxyToBitmap(imageProxy)
        val text = bitmap?.let { decode(it) }
        if (text != null) {
            onQrDetected(text)
        }
        imageProxy.close()
    }

    private fun decode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return try {
            MultiFormatReader()
                .apply { setHints(hints) }
                .decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels))))
                .text
        } catch (_: Exception) {
            null
        }
    }
}

/** Конвертирует кадр камеры (YUV_420_888) в ARGB [Bitmap], учитывая rotation. */
private fun proxyToBitmap(proxy: ImageProxy): Bitmap? {
    val yPlane = proxy.planes.getOrNull(0) ?: return null
    val uPlane = proxy.planes.getOrNull(1) ?: return null
    val vPlane = proxy.planes.getOrNull(2) ?: return null

    val width = proxy.width
    val height = proxy.height
    if (width <= 0 || height <= 0) return null

    val ySize = yPlane.buffer.remaining()
    val uvSize = (width * height) / 4
    val nv21 = ByteArray(ySize + uvSize * 2)

    // Y plane.
    val yBytes = ByteArray(ySize)
    yPlane.buffer.get(yBytes)
    System.arraycopy(yBytes, 0, nv21, 0, ySize)

    // UV interleaved as NV21 (VU).
    val uvBytes = ByteArray(uPlane.buffer.remaining() + vPlane.buffer.remaining())
    uPlane.buffer.get(uvBytes, 0, uPlane.buffer.remaining())
    vPlane.buffer.get(uvBytes, uPlane.buffer.remaining(), vPlane.buffer.remaining())
    val uvOut = ByteArray(uvSize * 2)
    val uRemaining = uPlane.buffer.remaining()
    for (i in 0 until uvSize) {
        uvOut[i * 2] = uvBytes[uRemaining + i] // V
        uvOut[i * 2 + 1] = uvBytes[i]          // U
    }
    System.arraycopy(uvOut, 0, nv21, ySize, uvOut.size)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val jpeg = out.toByteArray()

    val dst = android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
    return if (proxy.imageInfo.rotationDegrees != 0) {
        val matrix = android.graphics.Matrix().apply {
            postRotate(proxy.imageInfo.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(
            dst, 0, 0, dst.width, dst.height, matrix, false
        )
        if (rotated != dst) dst.recycle()
        rotated
    } else {
        dst
    }
}