package kr.joolabs.albumframe.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.domain.CameraLens
import kr.joolabs.albumframe.domain.PhotoFit
import kr.joolabs.albumframe.domain.SlideshowOrder
import kr.joolabs.albumframe.domain.SlideshowSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: SlideshowSettings,
    onSettingsChanged: (SlideshowSettings) -> Unit,
    onOpenScreensaverSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.slideshow_settings),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(24.dp))
            SettingLabel(R.string.interval)
            ChipRow {
                SlideshowSettings.INTERVAL_CHOICES.forEach { seconds ->
                    FilterChip(
                        selected = settings.intervalSeconds == seconds,
                        onClick = {
                            onSettingsChanged(settings.copy(intervalSeconds = seconds))
                        },
                        label = { Text(stringResource(R.string.seconds, seconds)) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SettingLabel(R.string.order)
            ChipRow {
                SlideshowOrder.entries.forEach { order ->
                    FilterChip(
                        selected = settings.order == order,
                        onClick = { onSettingsChanged(settings.copy(order = order)) },
                        label = {
                            Text(
                                stringResource(
                                    if (order == SlideshowOrder.CHRONOLOGICAL) {
                                        R.string.chronological
                                    } else {
                                        R.string.shuffled
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SettingLabel(R.string.photo_fit)
            ChipRow {
                PhotoFit.entries.forEach { fit ->
                    FilterChip(
                        selected = settings.fit == fit,
                        onClick = { onSettingsChanged(settings.copy(fit = fit)) },
                        label = {
                            Text(
                                stringResource(
                                    if (fit == PhotoFit.CONTAIN) {
                                        R.string.contain
                                    } else {
                                        R.string.cover
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SettingLabel(R.string.camera_display)
            ChipRow {
                FilterChip(
                    selected = !settings.cameraEnabled,
                    onClick = {
                        onSettingsChanged(settings.copy(cameraEnabled = false))
                    },
                    label = { Text(stringResource(R.string.photo_only)) },
                )
                FilterChip(
                    selected = settings.cameraEnabled,
                    onClick = {
                        onSettingsChanged(settings.copy(cameraEnabled = true))
                    },
                    label = { Text(stringResource(R.string.photo_and_camera)) },
                )
            }
            if (settings.cameraEnabled) {
                Spacer(Modifier.height(20.dp))
                SettingLabel(R.string.camera_direction)
                ChipRow {
                    CameraLens.entries.forEach { lens ->
                        FilterChip(
                            selected = settings.cameraLens == lens,
                            onClick = {
                                onSettingsChanged(settings.copy(cameraLens = lens))
                            },
                            label = {
                                Text(
                                    stringResource(
                                        if (lens == CameraLens.FRONT) {
                                            R.string.front_camera
                                        } else {
                                            R.string.back_camera
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SettingLabel(R.string.system_screensaver)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenScreensaverSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.DisplaySettings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_screensaver_settings))
            }
        }
    }
}

@Composable
private fun SettingLabel(stringResourceId: Int) {
    Text(
        text = stringResource(stringResourceId),
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
