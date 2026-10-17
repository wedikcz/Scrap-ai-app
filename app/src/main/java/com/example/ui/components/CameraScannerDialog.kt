package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Fullscreen modal dialog hosting the [CameraPreview] component
 * for scanning electronic components, SMD packages, and PCBs.
 */
@Composable
fun CameraScannerDialog(
    onDismiss: () -> Unit,
    onImageCaptured: (base64String: String, fileName: String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            initialScanMode = ComponentScanMode.SOIC_SOP,
            showOverlay = true,
            showControls = true,
            onClose = onDismiss,
            onImageCaptured = { base64, fileName ->
                onImageCaptured(base64, fileName)
            }
        )
    }
}
