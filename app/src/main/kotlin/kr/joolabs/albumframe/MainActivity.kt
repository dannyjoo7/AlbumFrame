package kr.joolabs.albumframe

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.net.toUri
import kr.joolabs.albumframe.application.PhotoAccessStatus
import kr.joolabs.albumframe.presentation.MainViewModel
import kr.joolabs.albumframe.presentation.MomentFrameApp

class MainActivity : ComponentActivity() {
    private val graph: AppGraph
        get() = (application as MomentFrameApplication).graph

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(graph)
    }

    private var photoAccessStatus by mutableStateOf(PhotoAccessStatus.NONE)
    private var cameraAvailable by mutableStateOf(false)
    private var cameraAccessGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshPhotoAccess()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshCameraAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            MomentFrameApp(
                viewModel = viewModel,
                photoAccessStatus = photoAccessStatus,
                cameraAvailable = cameraAvailable,
                cameraAccessGranted = cameraAccessGranted,
                onRequestPhotoAccess = {
                    permissionLauncher.launch(graph.photoAccess.requestPermissions())
                },
                onRequestCameraAccess = {
                    if (graph.cameraAccess.isAvailable()) {
                        cameraPermissionLauncher.launch(graph.cameraAccess.requestPermission())
                    }
                },
                onOpenAppSettings = ::openAppSettings,
                onOpenScreensaverSettings = ::openScreensaverSettings,
                onFullscreenChanged = ::setFullscreenPlayerMode,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAppActive(true)
        refreshPhotoAccess()
        refreshCameraAccess()
    }

    override fun onPause() {
        viewModel.setAppActive(false)
        super.onPause()
    }

    private fun refreshPhotoAccess() {
        photoAccessStatus = graph.photoAccess.status()
        viewModel.refreshAlbums(photoAccessStatus != PhotoAccessStatus.NONE)
    }

    private fun refreshCameraAccess() {
        cameraAvailable = graph.cameraAccess.isAvailable()
        cameraAccessGranted = graph.cameraAccess.isGranted()
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun openScreensaverSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }

    private fun setFullscreenPlayerMode(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
