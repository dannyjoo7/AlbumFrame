package kr.joolabs.albumframe.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kr.joolabs.albumframe.application.PhotoAccessStatus
import kr.joolabs.albumframe.domain.CameraLens
import kr.joolabs.albumframe.presentation.screen.AlbumScreen
import kr.joolabs.albumframe.presentation.screen.SettingsSheet
import kr.joolabs.albumframe.presentation.screen.SlideshowScreen
import kr.joolabs.albumframe.presentation.theme.MomentFrameTheme

@Composable
fun MomentFrameApp(
    viewModel: MainViewModel,
    photoAccessStatus: PhotoAccessStatus,
    cameraAvailable: Boolean,
    cameraAccessGranted: Boolean,
    onRequestPhotoAccess: () -> Unit,
    onRequestCameraAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenScreensaverSettings: () -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var settingsVisible by remember { mutableStateOf(false) }
    val player = state.player

    BackHandler(enabled = player != null) {
        viewModel.closePlayer()
    }

    LaunchedEffect(player != null) {
        onFullscreenChanged(player != null)
    }
    LaunchedEffect(state.settings.cameraEnabled, cameraAvailable, cameraAccessGranted) {
        if (state.settings.cameraEnabled && cameraAvailable && !cameraAccessGranted) {
            onRequestCameraAccess()
        }
    }
    DisposableEffect(settingsVisible) {
        viewModel.setSettingsVisible(settingsVisible)
        onDispose {
            if (settingsVisible) viewModel.setSettingsVisible(false)
        }
    }

    MomentFrameTheme {
        if (player == null) {
            AlbumScreen(
                state = state,
                photoAccessStatus = photoAccessStatus,
                onRequestPhotoAccess = onRequestPhotoAccess,
                onOpenAppSettings = onOpenAppSettings,
                onRetry = {
                    viewModel.refreshAlbums(photoAccessStatus != PhotoAccessStatus.NONE)
                },
                onChooseMorePhotos = onRequestPhotoAccess,
                onSelectAlbum = viewModel::selectAlbum,
                onOpenSlideshow = viewModel::openSelectedAlbum,
                onOpenSettings = { settingsVisible = true },
                loadThumbnail = viewModel::loadThumbnail,
            )
        } else {
            SlideshowScreen(
                player = player,
                settings = state.settings,
                cameraAvailable = cameraAvailable,
                cameraAccessGranted = cameraAccessGranted,
                onClose = viewModel::closePlayer,
                onToggleControls = viewModel::toggleControls,
                onPrevious = viewModel::showPrevious,
                onNext = { viewModel.showNext() },
                onTogglePlayback = viewModel::togglePlayback,
                onToggleCamera = {
                    viewModel.updateSettings(
                        state.settings.copy(
                            cameraEnabled = !state.settings.cameraEnabled,
                        ),
                    )
                },
                onToggleCameraLens = {
                    viewModel.updateSettings(
                        state.settings.copy(
                            cameraLens = if (
                                state.settings.cameraLens ==
                                CameraLens.FRONT
                            ) {
                                CameraLens.BACK
                            } else {
                                CameraLens.FRONT
                            },
                        ),
                    )
                },
                onRequestCameraAccess = onRequestCameraAccess,
                onOpenSettings = { settingsVisible = true },
            )
        }

        if (settingsVisible) {
            SettingsSheet(
                settings = state.settings,
                onSettingsChanged = viewModel::updateSettings,
                onOpenScreensaverSettings = onOpenScreensaverSettings,
                onDismiss = { settingsVisible = false },
            )
        }
    }
}
