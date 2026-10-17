package com.example.ui.components

import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Scanning target mode for electronic components.
 * Determines the aspect ratio and guide markings drawn by the overlay.
 */
enum class ComponentScanMode(
    val title: String,
    val aspectWidth: Float,
    val aspectHeight: Float,
    val leadDescription: String
) {
    SOIC_SOP("SOIC / SOP", 0.72f, 1.0f, "Pins: 8–16 (1.27mm / 50mil pitch)"),
    QFP_QFN("QFP / QFN", 1.0f, 1.0f, "Quad Flat (0.5mm / 0.8mm pitch)"),
    PASSIVE("SMD 0805", 1.6f, 1.0f, "2-Terminal R/C/L (0805 / 1206)"),
    DIP_MODULE("DIP / PCB", 0.6f, 1.0f, "Dual In-Line (2.54mm / 100mil)")
}

/**
 * CameraPreview UI component integrating CameraX with a high-tech electronic component scanning overlay.
 *
 * @param modifier Modifier for sizing and styling the preview container.
 * @param initialScanMode The initial package target shape.
 * @param showOverlay If true, renders the electronic component targeting HUD and laser scan line.
 * @param showControls If true, renders the top HUD (torch toggle, mode switch) and bottom shutter button.
 * @param onClose Optional callback invoked when the close button is clicked (if provided).
 * @param onImageCaptured Callback with the captured JPEG encoded as a Base64 string and file tag.
 * @param onError Callback for any camera initialization or capture exceptions.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    initialScanMode: ComponentScanMode = ComponentScanMode.SOIC_SOP,
    showOverlay: Boolean = true,
    showControls: Boolean = true,
    onClose: (() -> Unit)? = null,
    onImageCaptured: ((base64: String, fileName: String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedMode by remember { mutableStateOf(initialScanMode) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Laser scanning animated sweep
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_preview_container")
    ) {
        // 1. CameraX AndroidView Feed
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        val errorText = "Chyba kamery: ${exc.localizedMessage}"
                        errorMessage = errorText
                        onError?.invoke(errorText)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_surface_view")
        )

        // 2. Electronic Component Scanning Overlay (HUD & Laser Reticle)
        if (showOverlay) {
            ElectronicScanningOverlay(
                modifier = Modifier.fillMaxSize(),
                scanMode = selectedMode,
                laserProgress = laserProgress,
                isTorchOn = isTorchOn
            )
        }

        // 3. Optional Overlay Controls & HUD Bar
        if (showControls) {
            // Top HUD Bar: Close Button, Title/Telemetry, Torch Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_camera_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zavřít hledáček",
                            tint = TextPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CAMERAX PCB & IC SCANNER",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = selectedMode.leadDescription,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = {
                        val next = !isTorchOn
                        cameraControl?.enableTorch(next)
                        isTorchOn = next
                    },
                    modifier = Modifier.testTag("toggle_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Přísvit LED",
                        tint = if (isTorchOn) NeonAmber else TextPrimary
                    )
                }
            }

            // Package Mode Selector Chips (Top-Middle under HUD bar)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 68.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ComponentScanMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan else DarkCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMode = mode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("scan_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Shutter Button & Progress (Bottom Center)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = NeonCyan,
                        strokeWidth = 3.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            val capture = imageCapture
                            if (capture != null && !isCapturing && onImageCaptured != null) {
                                isCapturing = true
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            try {
                                                val bitmap = imageProxy.toBitmap()
                                                val outputStream = ByteArrayOutputStream()
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                                                val bytes = outputStream.toByteArray()
                                                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                val fileName = "PCB_${selectedMode.name}_${System.currentTimeMillis() % 10000}.jpg"

                                                ContextCompat.getMainExecutor(context).execute {
                                                    isCapturing = false
                                                    onImageCaptured.invoke(base64, fileName)
                                                }
                                            } catch (exc: Exception) {
                                                ContextCompat.getMainExecutor(context).execute {
                                                    isCapturing = false
                                                    val err = "Chyba konverze snímku: ${exc.localizedMessage}"
                                                    errorMessage = err
                                                    onError?.invoke(err)
                                                }
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                val err = "Chyba fotoaparátu: ${exception.localizedMessage}"
                                                errorMessage = err
                                                onError?.invoke(err)
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .border(3.dp, Color.White, CircleShape)
                            .testTag("camera_preview_shutter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Zachytit součástku",
                            tint = DarkBase,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        // Error message banner
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = NeonRose,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonRose.copy(alpha = 0.2f))
                            .clickable { errorMessage = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Rozumím",
                            color = NeonRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated visual overlay for scanning electronic components and PCBs.
 *
 * Renders:
 * - Dimmed outer vignette with transparent component cutout.
 * - Precision cyan corner brackets.
 * - Component pin markers (simulating IC package leads on left and right borders).
 * - Orientation index mark: Pin 1 dot / notch indicator at top-left corner.
 * - Central precision reticle crosshair.
 * - Animated laser scan sweep with neon glow.
 * - Engineering telemetry labels (Package mode, pitch calibration, focus lock).
 */
@Composable
fun ElectronicScanningOverlay(
    modifier: Modifier = Modifier,
    scanMode: ComponentScanMode,
    laserProgress: Float,
    isTorchOn: Boolean
) {
    BoxWithConstraints(modifier = modifier) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // Calculate reticle dimensions based on component mode aspect ratio
        val baseSize = minOf(totalWidth * 0.72f, totalHeight * 0.45f)
        val rectWidth = baseSize * scanMode.aspectWidth
        val rectHeight = baseSize * scanMode.aspectHeight

        val left = (totalWidth - rectWidth) / 2f
        val top = (totalHeight - rectHeight) / 2f
        val right = left + rectWidth
        val bottom = top + rectHeight

        Canvas(modifier = Modifier.fillMaxSize().testTag("electronic_scanner_canvas")) {
            val cutOutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
            }

            // 1. Dimmed outer boundary
            clipPath(cutOutPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.65f))
            }

            // 2. High-tech reticle border
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.75f),
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Precision Corner Brackets (Accentuated)
            val cornerLength = 28.dp.toPx()
            val bracketStroke = 4.dp.toPx()

            // Top-Left corner
            drawLine(
                color = NeonEmerald,
                start = Offset(left, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = bracketStroke
            )
            drawLine(
                color = NeonEmerald,
                start = Offset(left, top),
                end = Offset(left, top + cornerLength),
                strokeWidth = bracketStroke
            )

            // Top-Right corner
            drawLine(
                color = NeonEmerald,
                start = Offset(right, top),
                end = Offset(right - cornerLength, top),
                strokeWidth = bracketStroke
            )
            drawLine(
                color = NeonEmerald,
                start = Offset(right, top),
                end = Offset(right, top + cornerLength),
                strokeWidth = bracketStroke
            )

            // Bottom-Left corner
            drawLine(
                color = NeonEmerald,
                start = Offset(left, bottom),
                end = Offset(left + cornerLength, bottom),
                strokeWidth = bracketStroke
            )
            drawLine(
                color = NeonEmerald,
                start = Offset(left, bottom),
                end = Offset(left, bottom - cornerLength),
                strokeWidth = bracketStroke
            )

            // Bottom-Right corner
            drawLine(
                color = NeonEmerald,
                start = Offset(right, bottom),
                end = Offset(right - cornerLength, bottom),
                strokeWidth = bracketStroke
            )
            drawLine(
                color = NeonEmerald,
                start = Offset(right, bottom),
                end = Offset(right, bottom - cornerLength),
                strokeWidth = bracketStroke
            )

            // 4. Pin 1 Dot Marker (Top-Left of IC chip for component orientation)
            drawCircle(
                color = NeonEmerald,
                radius = 5.dp.toPx(),
                center = Offset(left + 16.dp.toPx(), top + 16.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(left + 16.dp.toPx(), top + 16.dp.toPx())
            )

            // 5. Electronic Package Lead Marks (Simulated IC pins on sides)
            val pinCountPerSide = when (scanMode) {
                ComponentScanMode.SOIC_SOP -> 8
                ComponentScanMode.QFP_QFN -> 10
                ComponentScanMode.PASSIVE -> 2
                ComponentScanMode.DIP_MODULE -> 8
            }
            val pinSpacingY = (rectHeight - 32.dp.toPx()) / (pinCountPerSide + 1)
            val pinLeadLength = 10.dp.toPx()

            for (i in 1..pinCountPerSide) {
                val py = top + 16.dp.toPx() + (i * pinSpacingY)

                // Left pin ticks
                drawLine(
                    color = NeonCyan.copy(alpha = 0.6f),
                    start = Offset(left - pinLeadLength, py),
                    end = Offset(left, py),
                    strokeWidth = 2.dp.toPx()
                )

                // Right pin ticks
                drawLine(
                    color = NeonCyan.copy(alpha = 0.6f),
                    start = Offset(right, py),
                    end = Offset(right + pinLeadLength, py),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // For QFP packages, also draw top and bottom leads
            if (scanMode == ComponentScanMode.QFP_QFN) {
                val pinSpacingX = (rectWidth - 32.dp.toPx()) / (pinCountPerSide + 1)
                for (i in 1..pinCountPerSide) {
                    val px = left + 16.dp.toPx() + (i * pinSpacingX)
                    // Top pins
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.6f),
                        start = Offset(px, top - pinLeadLength),
                        end = Offset(px, top),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Bottom pins
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.6f),
                        start = Offset(px, bottom),
                        end = Offset(px, bottom + pinLeadLength),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // 6. Central Optical Crosshairs
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val crossLength = 12.dp.toPx()

            drawLine(
                color = NeonCyan.copy(alpha = 0.4f),
                start = Offset(centerX - crossLength, centerY),
                end = Offset(centerX + crossLength, centerY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.4f),
                start = Offset(centerX, centerY - crossLength),
                end = Offset(centerX, centerY + crossLength),
                strokeWidth = 1.5.dp.toPx()
            )

            // 7. Animated Cyber Laser Sweep with Gradient Glow
            val laserY = top + (rectHeight * laserProgress)

            // Laser beam glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonCyan.copy(alpha = 0.25f),
                        NeonCyan,
                        NeonCyan.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = laserY - 14.dp.toPx(),
                    endY = laserY + 14.dp.toPx()
                ),
                topLeft = Offset(left, laserY - 14.dp.toPx()),
                size = Size(rectWidth, 28.dp.toPx())
            )

            // Main laser line
            drawLine(
                color = Color.White,
                start = Offset(left + 4.dp.toPx(), laserY),
                end = Offset(right - 4.dp.toPx(), laserY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 8. Electronic Component HUD Annotations (Text Badges)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = (rectHeight / 2f + 24).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = DarkBase.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) NeonAmber else NeonEmerald)
                    )
                    Text(
                        text = "ALIGN IC: PIN 1 NOTCH @ TOP-LEFT",
                        color = NeonEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
