package kr.joolabs.albumframe.presentation.screen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.domain.PhotoFit
import kr.joolabs.albumframe.domain.SlideshowSettings
import kr.joolabs.albumframe.presentation.PlayerUiState
import kr.joolabs.albumframe.presentation.camera.CameraPreviewPane
import kr.joolabs.albumframe.presentation.theme.Accent
import kr.joolabs.albumframe.presentation.theme.Background
import kr.joolabs.albumframe.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SlideshowScreen(
    player: PlayerUiState,
    settings: SlideshowSettings,
    cameraAvailable: Boolean,
    cameraAccessGranted: Boolean,
    onClose: () -> Unit,
    onToggleControls: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onRequestCameraAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount
                    },
                    onDragEnd = {
                        if (abs(dragDistance) >= SWIPE_DISTANCE_PX) {
                            if (dragDistance < 0) onNext() else onPrevious()
                        }
                    },
                )
            },
    ) {
        val isTablet = maxWidth >= 600.dp
        FrameContent(
            player = player,
            settings = settings,
            isTablet = isTablet,
            cameraAvailable = cameraAvailable,
            cameraAccessGranted = cameraAccessGranted,
            onRequestCameraAccess = onRequestCameraAccess,
        )
        AnimatedVisibility(
            visible = player.controlsVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) {
            PlayerControls(
                player = player,
                onClose = onClose,
                onPrevious = onPrevious,
                onNext = onNext,
                onTogglePlayback = onTogglePlayback,
                cameraEnabled = settings.cameraEnabled,
                onToggleCamera = onToggleCamera,
                onToggleCameraLens = onToggleCameraLens,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun FrameContent(
    player: PlayerUiState,
    settings: SlideshowSettings,
    isTablet: Boolean,
    cameraAvailable: Boolean,
    cameraAccessGranted: Boolean,
    onRequestCameraAccess: () -> Unit,
) {
    if (settings.cameraEnabled) {
        Row(Modifier.fillMaxSize()) {
            PhotoPane(
                player = player,
                fit = settings.fit,
                isTablet = isTablet,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            CameraPreviewPane(
                lens = settings.cameraLens,
                cameraAvailable = cameraAvailable,
                cameraAccessGranted = cameraAccessGranted,
                onRequestCameraAccess = onRequestCameraAccess,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    } else {
        PhotoPane(
            player = player,
            fit = settings.fit,
            isTablet = isTablet,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PhotoPane(
    player: PlayerUiState,
    fit: PhotoFit,
    isTablet: Boolean,
    modifier: Modifier,
) {
    Box(modifier) {
        PhotoCanvas(player, fit, Modifier.fillMaxSize())
        AmbientClock(
            isTablet = isTablet,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = if (isTablet) 40.dp else 24.dp,
                    bottom = if (isTablet) 44.dp else 32.dp,
                ),
        )
    }
}

@Composable
private fun PhotoCanvas(
    player: PlayerUiState,
    fit: PhotoFit,
    modifier: Modifier,
) {
    val bitmap = player.bitmap
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            if (player.imageUnavailable) {
                Text(
                    stringResource(R.string.image_unavailable),
                    color = TextSecondary,
                )
            } else {
                CircularProgressIndicator()
            }
        } else {
            Crossfade(
                targetState = bitmap,
                animationSpec = tween(500),
                label = "photoCrossfade",
            ) { activeBitmap ->
                if (fit == PhotoFit.COVER) {
                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                    ) {
                        val backdropEffect = if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ) {
                            Modifier.blur(28.dp)
                        } else {
                            Modifier
                        }
                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.12f)
                                .then(backdropEffect),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                        )
                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
        if (player.loading && bitmap != null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun AmbientClock(isTablet: Boolean, modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            val millis = System.currentTimeMillis()
            delay(60_000L - millis % 60_000L + 50L)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    val dateFormat = remember { SimpleDateFormat("M월 d일 · EEEE", Locale.KOREAN) }
    Column(modifier) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = if (isTablet) 78.sp else 54.sp,
            fontWeight = FontWeight.Light,
            lineHeight = if (isTablet) 82.sp else 58.sp,
        )
        Text(
            text = dateFormat.format(now),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = if (isTablet) 18.sp else 15.sp,
        )
    }
}

@Composable
private fun PlayerControls(
    player: PlayerUiState,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayback: () -> Unit,
    cameraEnabled: Boolean,
    onToggleCamera: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.72f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
            )
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerButton(
                icon = Icons.Rounded.Close,
                description = stringResource(R.string.close),
                onClick = onClose,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = player.session.album.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.player_progress,
                        player.position + 1,
                        player.session.photoIds.size,
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            PlayerButton(
                icon = if (cameraEnabled) {
                    Icons.Rounded.VideocamOff
                } else {
                    Icons.Rounded.Videocam
                },
                description = stringResource(
                    if (cameraEnabled) {
                        R.string.turn_camera_off
                    } else {
                        R.string.turn_camera_on
                    },
                ),
                onClick = onToggleCamera,
            )
            Spacer(Modifier.width(10.dp))
            if (cameraEnabled) {
                PlayerButton(
                    icon = Icons.Rounded.Cameraswitch,
                    description = stringResource(R.string.switch_camera),
                    onClick = onToggleCameraLens,
                )
                Spacer(Modifier.width(10.dp))
            }
            PlayerButton(
                icon = Icons.Rounded.Settings,
                description = stringResource(R.string.slideshow_settings),
                onClick = onOpenSettings,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerButton(
                icon = Icons.Rounded.SkipPrevious,
                description = stringResource(R.string.previous_photo),
                onClick = onPrevious,
            )
            PlayerButton(
                icon = if (player.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = stringResource(
                    if (player.playing) R.string.pause else R.string.play,
                ),
                emphasized = true,
                onClick = onTogglePlayback,
            )
            PlayerButton(
                icon = Icons.Rounded.SkipNext,
                description = stringResource(R.string.next_photo),
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = if (emphasized) Accent else Color.Black.copy(alpha = 0.62f),
        modifier = Modifier.size(if (emphasized) 64.dp else 56.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (emphasized) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

private const val SWIPE_DISTANCE_PX = 100f
