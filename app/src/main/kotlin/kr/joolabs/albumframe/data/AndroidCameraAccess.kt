package kr.joolabs.albumframe.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kr.joolabs.albumframe.application.CameraAccess

class AndroidCameraAccess(private val context: Context) : CameraAccess {
    override fun isAvailable(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    override fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun requestPermission(): String = Manifest.permission.CAMERA
}
