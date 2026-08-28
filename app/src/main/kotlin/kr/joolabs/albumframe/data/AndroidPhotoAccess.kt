package kr.joolabs.albumframe.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kr.joolabs.albumframe.application.PhotoAccessStatus

class AndroidPhotoAccess(private val context: Context) {
    fun status(): PhotoAccessStatus = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(Manifest.permission.READ_MEDIA_IMAGES) -> PhotoAccessStatus.FULL
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> {
            PhotoAccessStatus.LIMITED
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            isGranted(Manifest.permission.READ_MEDIA_IMAGES) -> PhotoAccessStatus.FULL
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) -> PhotoAccessStatus.FULL
        else -> PhotoAccessStatus.NONE
    }

    fun canReadPhotos(): Boolean = status() != PhotoAccessStatus.NONE

    fun requestPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
}
