package kr.joolabs.albumframe.application

import kr.joolabs.albumframe.domain.SlideshowSettings

interface SettingsRepository {
    fun load(): SlideshowSettings

    fun save(settings: SlideshowSettings)
}

class UnsupportedSettingsSchemaException(version: Int) :
    IllegalStateException("Unsupported settings schema version: $version")
