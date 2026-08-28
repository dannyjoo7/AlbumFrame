package kr.joolabs.albumframe.data

import android.content.Context
import androidx.core.content.edit
import kr.joolabs.albumframe.application.SettingsRepository
import kr.joolabs.albumframe.application.UnsupportedSettingsSchemaException
import kr.joolabs.albumframe.domain.CameraLens
import kr.joolabs.albumframe.domain.PhotoFit
import kr.joolabs.albumframe.domain.SlideshowOrder
import kr.joolabs.albumframe.domain.SlideshowSettings

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences(
        PreferenceSchema.FILE_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): SlideshowSettings {
        val version = preferences.getLong(
            PreferenceSchema.SCHEMA_VERSION,
            PreferenceSchema.CURRENT_VERSION.toLong(),
        ).toInt()
        if (version != PreferenceSchema.CURRENT_VERSION) {
            throw UnsupportedSettingsSchemaException(version)
        }
        val interval = preferences.getLong(
            PreferenceSchema.INTERVAL_SECONDS,
            SlideshowSettings.DEFAULT_INTERVAL_SECONDS.toLong(),
        ).toInt().takeIf(SlideshowSettings.INTERVAL_CHOICES::contains)
            ?: SlideshowSettings.DEFAULT_INTERVAL_SECONDS
        return SlideshowSettings(
            selectedAlbumId = preferences.getString(PreferenceSchema.SELECTED_ALBUM_ID, null),
            intervalSeconds = interval,
            order = if (
                preferences.getString(
                    PreferenceSchema.ORDER,
                    PreferenceSchema.CHRONOLOGICAL,
                ) == PreferenceSchema.SHUFFLED
            ) {
                SlideshowOrder.SHUFFLED
            } else {
                SlideshowOrder.CHRONOLOGICAL
            },
            fit = if (
                preferences.getString(
                    PreferenceSchema.FIT,
                    PreferenceSchema.CONTAIN,
                ) == PreferenceSchema.COVER
            ) {
                PhotoFit.COVER
            } else {
                PhotoFit.CONTAIN
            },
            cameraEnabled = preferences.getBoolean(PreferenceSchema.CAMERA_ENABLED, true),
            cameraLens = if (
                preferences.getString(
                    PreferenceSchema.CAMERA_LENS,
                    PreferenceSchema.FRONT,
                ) == PreferenceSchema.BACK
            ) {
                CameraLens.BACK
            } else {
                CameraLens.FRONT
            },
        )
    }

    override fun save(settings: SlideshowSettings) {
        preferences.edit {
            if (settings.selectedAlbumId == null) {
                remove(PreferenceSchema.SELECTED_ALBUM_ID)
            } else {
                putString(PreferenceSchema.SELECTED_ALBUM_ID, settings.selectedAlbumId)
            }
            putLong(PreferenceSchema.INTERVAL_SECONDS, settings.intervalSeconds.toLong())
            putString(
                PreferenceSchema.ORDER,
                if (settings.order == SlideshowOrder.SHUFFLED) {
                    PreferenceSchema.SHUFFLED
                } else {
                    PreferenceSchema.CHRONOLOGICAL
                },
            )
            putString(
                PreferenceSchema.FIT,
                if (settings.fit == PhotoFit.COVER) {
                    PreferenceSchema.COVER
                } else {
                    PreferenceSchema.CONTAIN
                },
            )
            putBoolean(PreferenceSchema.CAMERA_ENABLED, settings.cameraEnabled)
            putString(
                PreferenceSchema.CAMERA_LENS,
                if (settings.cameraLens == CameraLens.BACK) {
                    PreferenceSchema.BACK
                } else {
                    PreferenceSchema.FRONT
                },
            )
            putLong(
                PreferenceSchema.SCHEMA_VERSION,
                PreferenceSchema.CURRENT_VERSION.toLong(),
            )
        }
    }
}

object PreferenceSchema {
    const val CURRENT_VERSION = 1
    const val FILE_NAME = "MomentFramePreferences"

    const val SCHEMA_VERSION = "schema_version"
    const val SELECTED_ALBUM_ID = "selected_album_id"
    const val INTERVAL_SECONDS = "interval_seconds"
    const val ORDER = "slideshow_order"
    const val FIT = "photo_fit"
    const val CAMERA_ENABLED = "camera_enabled"
    const val CAMERA_LENS = "camera_lens"

    const val CHRONOLOGICAL = "chronological"
    const val SHUFFLED = "shuffled"
    const val CONTAIN = "contain"
    const val COVER = "cover"
    const val FRONT = "front"
    const val BACK = "back"
}
