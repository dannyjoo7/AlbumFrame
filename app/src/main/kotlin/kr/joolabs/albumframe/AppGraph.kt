package kr.joolabs.albumframe

import android.content.Context
import kr.joolabs.albumframe.application.PhotoLibrary
import kr.joolabs.albumframe.application.SettingsRepository
import kr.joolabs.albumframe.data.AndroidCameraAccess
import kr.joolabs.albumframe.data.AndroidPhotoAccess
import kr.joolabs.albumframe.data.MediaStorePhotoLibrary
import kr.joolabs.albumframe.data.SharedPreferencesSettingsRepository

class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val photoLibrary: PhotoLibrary by lazy { MediaStorePhotoLibrary(appContext) }
    val settingsRepository: SettingsRepository by lazy {
        SharedPreferencesSettingsRepository(appContext)
    }
    val cameraAccess: AndroidCameraAccess by lazy { AndroidCameraAccess(appContext) }
    val photoAccess: AndroidPhotoAccess by lazy { AndroidPhotoAccess(appContext) }
}
