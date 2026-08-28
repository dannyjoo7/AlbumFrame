package kr.joolabs.albumframe.dream

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kr.joolabs.albumframe.MomentFrameApplication
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.domain.PhotoPlaylist
import kr.joolabs.albumframe.domain.SlideshowSettings
import kr.joolabs.albumframe.dream.camera.DreamCameraController
import kr.joolabs.albumframe.dream.presentation.DreamFrameView
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/** 시스템이 충전 또는 도킹 중 실행하는 네이티브 모먼트 프레임 화면보호기다. */
class MomentFrameDreamService : DreamService(), LifecycleOwner {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    private val graph by lazy {
        (application as MomentFrameApplication).graph
    }
    private lateinit var frameView: DreamFrameView
    private lateinit var cameraController: DreamCameraController
    private var runGeneration = 0
    private var settings: SlideshowSettings? = null
    private var playlist: PhotoPlaylist<String>? = null
    private var failedPhotos = 0

    private val clockRunnable = object : Runnable {
        override fun run() {
            if (!::frameView.isInitialized) return
            frameView.updateClock(Date())
            val now = System.currentTimeMillis()
            val delay = CLOCK_UPDATE_MILLIS - (now % CLOCK_UPDATE_MILLIS) +
                CLOCK_TOLERANCE_MILLIS
            mainHandler.postDelayed(this, delay)
        }
    }

    private val nextPhotoRunnable = Runnable {
        playlist?.next()
        loadCurrentPhoto(runGeneration)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true
        frameView = DreamFrameView(this)
        cameraController = DreamCameraController(
            context = this,
            lifecycleOwner = this,
            previewView = frameView.cameraPreviewView,
            onFailure = {
                frameView.showCameraMessage(getString(R.string.camera_open_failed))
            },
        )
        setContentView(frameView)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        val generation = ++runGeneration
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        mainHandler.removeCallbacksAndMessages(null)
        clockRunnable.run()
        loadConfiguration(generation)
    }

    override fun onDreamingStopped() {
        runGeneration++
        mainHandler.removeCallbacksAndMessages(null)
        settings = null
        playlist = null
        if (::cameraController.isInitialized) cameraController.stop()
        if (::frameView.isInitialized) {
            frameView.clearPhotos()
            frameView.setCameraEnabled(false)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        if (::cameraController.isInitialized) cameraController.stop()
        if (::frameView.isInitialized) frameView.clearPhotos()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun loadConfiguration(generation: Int) {
        executor.execute {
            val loadedSettings = runCatching {
                graph.settingsRepository.load()
            }.getOrElse {
                postMessage(generation, R.string.dream_settings_error)
                return@execute
            }
            mainHandler.post {
                if (generation != runGeneration) return@post
                settings = loadedSettings
                configureCamera(loadedSettings)
            }
            val albumId = loadedSettings.selectedAlbumId
            if (albumId == null) {
                postMessage(generation, R.string.dream_album_required)
                return@execute
            }
            if (!graph.photoAccess.canReadPhotos()) {
                postMessage(generation, R.string.dream_permission_required)
                return@execute
            }
            val photos = runCatching {
                graph.photoLibrary.listPhotoIds(albumId)
            }.getOrElse {
                postMessage(generation, R.string.dream_photo_error)
                return@execute
            }
            if (photos.isEmpty()) {
                postMessage(generation, R.string.dream_empty_album)
                return@execute
            }
            val loadedPlaylist = PhotoPlaylist(photos, loadedSettings.order)
            mainHandler.post {
                if (generation != runGeneration) return@post
                playlist = loadedPlaylist
                failedPhotos = 0
                loadCurrentPhoto(generation)
            }
        }
    }

    private fun configureCamera(activeSettings: SlideshowSettings) {
        if (!activeSettings.cameraEnabled) {
            cameraController.stop()
            frameView.setCameraEnabled(false)
            return
        }
        frameView.setCameraEnabled(true)
        when {
            !graph.cameraAccess.isAvailable() -> {
                cameraController.stop()
                frameView.showCameraMessage(getString(R.string.camera_unavailable))
            }
            !graph.cameraAccess.isGranted() -> {
                cameraController.stop()
                frameView.showCameraMessage(getString(R.string.camera_permission_required))
            }
            else -> {
                frameView.clearCameraMessage()
                cameraController.start(activeSettings.cameraLens)
            }
        }
    }

    private fun loadCurrentPhoto(generation: Int) {
        mainHandler.removeCallbacks(nextPhotoRunnable)
        val currentPlaylist = playlist ?: return
        val photoId = currentPlaylist.current
        val maxDimension = max(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels,
        ).coerceAtLeast(MIN_IMAGE_DIMENSION)
        executor.execute {
            val bitmap = runCatching {
                graph.photoLibrary.loadImage(photoId, maxDimension)
            }.getOrNull()
            mainHandler.post {
                if (generation != runGeneration) return@post
                if (bitmap == null) {
                    failedPhotos++
                    if (failedPhotos >= currentPlaylist.size) {
                        frameView.showMessage(getString(R.string.dream_photo_error))
                    } else {
                        currentPlaylist.next()
                        loadCurrentPhoto(generation)
                    }
                    return@post
                }
                val activeSettings = settings ?: return@post
                failedPhotos = 0
                frameView.showPhoto(bitmap, activeSettings.fit)
                mainHandler.postDelayed(
                    nextPhotoRunnable,
                    activeSettings.intervalSeconds * 1_000L,
                )
            }
        }
    }

    private fun postMessage(generation: Int, @StringRes message: Int) {
        mainHandler.post {
            if (generation == runGeneration) frameView.showMessage(getString(message))
        }
    }

    private companion object {
        const val CLOCK_UPDATE_MILLIS = 60_000L
        const val CLOCK_TOLERANCE_MILLIS = 50L
        const val MIN_IMAGE_DIMENSION = 1_080
    }
}
