package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * CameraPermission utility class to manage camera permissions for the component scanner feature.
 * Supports runtime permission checks, rationale detection, and Jetpack Compose launcher integration.
 */
class CameraPermission(private val context: Context) {

    /**
     * Check if camera permission is granted.
     */
    fun hasPermission(): Boolean {
        return hasCameraPermission(context)
    }

    /**
     * Check if permission rationale should be displayed.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)
    }

    companion object {
        const val PERMISSION: String = Manifest.permission.CAMERA

        /**
         * Static helper to check camera permission.
         */
        fun hasCameraPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Composable helper to remember a camera permission launcher.
         */
        @Composable
        fun rememberCameraPermissionLauncher(
            onPermissionResult: (isGranted: Boolean) -> Unit
        ) = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = onPermissionResult
        )
    }
}
