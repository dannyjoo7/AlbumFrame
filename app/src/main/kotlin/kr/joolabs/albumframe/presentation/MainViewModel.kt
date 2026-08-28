package kr.joolabs.albumframe.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.joolabs.albumframe.AppGraph
import kr.joolabs.albumframe.application.PhotoLibrary
import kr.joolabs.albumframe.application.SettingsRepository
import kr.joolabs.albumframe.domain.PhotoAlbum
import kr.joolabs.albumframe.domain.PhotoPlaylist
import kr.joolabs.albumframe.domain.SlideshowOrder
import kr.joolabs.albumframe.domain.SlideshowSession
import kr.joolabs.albumframe.domain.SlideshowSettings

enum class HomeFailure {
    ALBUMS,
    EMPTY_ALBUM,
}

data class PlayerUiState(
    val session: SlideshowSession,
    val bitmap: Bitmap? = null,
    val position: Int = 0,
    val playing: Boolean = true,
    val controlsVisible: Boolean = false,
    val loading: Boolean = true,
    val imageUnavailable: Boolean = false,
)

data class MomentFrameUiState(
    val settings: SlideshowSettings = SlideshowSettings(),
    val albums: List<PhotoAlbum> = emptyList(),
    val loadingAlbums: Boolean = false,
    val startingSlideshow: Boolean = false,
    val failure: HomeFailure? = null,
    val player: PlayerUiState? = null,
)

class MainViewModel(
    private val photoLibrary: PhotoLibrary,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        MomentFrameUiState(settings = loadSettings()),
    )
    val state: StateFlow<MomentFrameUiState> = mutableState.asStateFlow()

    private var playlist: PhotoPlaylist<String>? = null
    private var albumJob: Job? = null
    private var imageJob: Job? = null
    private var advanceJob: Job? = null
    private var controlsJob: Job? = null
    private var imageGeneration = 0
    private var failedPhotosInCycle = 0
    private var appActive = true
    private var settingsVisible = false

    fun refreshAlbums(canReadPhotos: Boolean) {
        albumJob?.cancel()
        if (!canReadPhotos) {
            mutableState.update {
                it.copy(albums = emptyList(), loadingAlbums = false, failure = null)
            }
            return
        }
        mutableState.update { it.copy(loadingAlbums = true, failure = null) }
        albumJob = viewModelScope.launch {
            val albums = runCatching {
                withContext(ioDispatcher) { photoLibrary.listAlbums() }
            }
            mutableState.update { current ->
                albums.fold(
                    onSuccess = {
                        current.copy(albums = it, loadingAlbums = false, failure = null)
                    },
                    onFailure = {
                        current.copy(loadingAlbums = false, failure = HomeFailure.ALBUMS)
                    },
                )
            }
        }
    }

    fun selectAlbum(album: PhotoAlbum) {
        val settings = state.value.settings.copy(selectedAlbumId = album.id)
        mutableState.update { it.copy(settings = settings, failure = null) }
        saveSettings(settings)
    }

    fun openSelectedAlbum() {
        if (state.value.startingSlideshow) return
        val album = state.value.albums.firstOrNull {
            it.id == state.value.settings.selectedAlbumId
        } ?: return
        mutableState.update { it.copy(startingSlideshow = true, failure = null) }
        viewModelScope.launch {
            val photos = runCatching {
                withContext(ioDispatcher) { photoLibrary.listPhotoIds(album.id) }
            }.getOrElse {
                mutableState.update {
                    it.copy(startingSlideshow = false, failure = HomeFailure.ALBUMS)
                }
                return@launch
            }
            if (photos.isEmpty()) {
                mutableState.update {
                    it.copy(startingSlideshow = false, failure = HomeFailure.EMPTY_ALBUM)
                }
                return@launch
            }
            playlist = PhotoPlaylist(photos, state.value.settings.order)
            failedPhotosInCycle = 0
            mutableState.update {
                it.copy(
                    startingSlideshow = false,
                    failure = null,
                    player = PlayerUiState(SlideshowSession(album, photos)),
                )
            }
            loadCurrentPhoto()
        }
    }

    fun closePlayer() {
        imageGeneration++
        imageJob?.cancel()
        advanceJob?.cancel()
        controlsJob?.cancel()
        failedPhotosInCycle = 0
        playlist = null
        mutableState.update { it.copy(player = null) }
    }

    fun showNext(revealControls: Boolean = true) {
        playlist?.next() ?: return
        preparePhotoChange(revealControls)
    }

    fun showPrevious() {
        playlist?.previous() ?: return
        preparePhotoChange(revealControls = true)
    }

    fun togglePlayback() {
        val player = state.value.player ?: return
        val playing = !player.playing
        mutableState.update {
            it.copy(player = player.copy(playing = playing, controlsVisible = true))
        }
        if (playing) scheduleAdvance() else advanceJob?.cancel()
        scheduleControlsHide()
    }

    fun toggleControls() {
        val player = state.value.player ?: return
        val visible = !player.controlsVisible
        mutableState.update { it.copy(player = player.copy(controlsVisible = visible)) }
        if (visible) scheduleControlsHide() else controlsJob?.cancel()
    }

    fun updateSettings(settings: SlideshowSettings) {
        val previous = state.value.settings
        mutableState.update { current ->
            val player = current.player
            current.copy(
                settings = settings,
                player = if (player != null && previous.order != settings.order) {
                    playlist?.changeOrder(settings.order)
                    player.copy(position = playlist?.position ?: player.position)
                } else {
                    player
                },
            )
        }
        saveSettings(settings)
        scheduleAdvance()
    }

    fun setSettingsVisible(visible: Boolean) {
        settingsVisible = visible
        if (visible) {
            advanceJob?.cancel()
            controlsJob?.cancel()
        } else {
            mutableState.value.player?.let { player ->
                mutableState.update {
                    it.copy(player = player.copy(controlsVisible = true))
                }
                scheduleAdvance()
                scheduleControlsHide()
            }
        }
    }

    fun setAppActive(active: Boolean) {
        appActive = active
        if (active) {
            scheduleAdvance()
        } else {
            advanceJob?.cancel()
            controlsJob?.cancel()
        }
    }

    suspend fun loadThumbnail(photoId: String): Bitmap? = withContext(ioDispatcher) {
        runCatching {
            photoLibrary.loadImage(photoId, THUMBNAIL_DIMENSION)
        }.getOrNull()
    }

    private fun preparePhotoChange(revealControls: Boolean) {
        advanceJob?.cancel()
        failedPhotosInCycle = 0
        val player = state.value.player ?: return
        mutableState.update {
            it.copy(
                player = player.copy(
                    position = playlist?.position ?: player.position,
                    loading = true,
                    imageUnavailable = false,
                    controlsVisible = revealControls || player.controlsVisible,
                ),
            )
        }
        if (revealControls) scheduleControlsHide()
        loadCurrentPhoto()
    }

    private fun loadCurrentPhoto(cancelPrevious: Boolean = true) {
        if (cancelPrevious) imageJob?.cancel()
        val photoId = playlist?.current ?: return
        val generation = ++imageGeneration
        imageJob = viewModelScope.launch {
            val bitmap = withContext(ioDispatcher) {
                runCatching {
                    photoLibrary.loadImage(photoId, DISPLAY_DIMENSION)
                }.getOrNull()
            }
            if (generation != imageGeneration) return@launch
            val player = state.value.player ?: return@launch
            if (bitmap == null) {
                val activePlaylist = playlist ?: return@launch
                failedPhotosInCycle++
                if (failedPhotosInCycle < activePlaylist.size) {
                    activePlaylist.next()
                    mutableState.update {
                        it.copy(
                            player = player.copy(
                                position = activePlaylist.position,
                                loading = true,
                                imageUnavailable = false,
                            ),
                        )
                    }
                    loadCurrentPhoto(cancelPrevious = false)
                    return@launch
                }
            } else {
                failedPhotosInCycle = 0
            }
            mutableState.update {
                it.copy(
                    player = player.copy(
                        bitmap = bitmap,
                        position = playlist?.position ?: player.position,
                        loading = false,
                        imageUnavailable = bitmap == null,
                    ),
                )
            }
            scheduleAdvance()
        }
    }

    private fun scheduleAdvance() {
        advanceJob?.cancel()
        val player = state.value.player ?: return
        if (!player.playing || !appActive || settingsVisible || player.loading) return
        advanceJob = viewModelScope.launch {
            delay(state.value.settings.intervalSeconds * 1_000L)
            showNext(revealControls = false)
        }
    }

    private fun scheduleControlsHide() {
        controlsJob?.cancel()
        val player = state.value.player ?: return
        if (!player.controlsVisible || settingsVisible || !appActive) return
        controlsJob = viewModelScope.launch {
            delay(CONTROLS_VISIBLE_MILLIS)
            val currentPlayer = state.value.player ?: return@launch
            mutableState.update {
                it.copy(player = currentPlayer.copy(controlsVisible = false))
            }
        }
    }

    private fun saveSettings(settings: SlideshowSettings) {
        viewModelScope.launch(ioDispatcher) {
            runCatching { settingsRepository.save(settings) }
        }
    }

    private fun loadSettings(): SlideshowSettings = runCatching {
        settingsRepository.load()
    }.getOrElse {
        SlideshowSettings().also(settingsRepository::save)
    }

    class Factory(private val graph: AppGraph) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(graph.photoLibrary, graph.settingsRepository) as T
    }

    private companion object {
        const val THUMBNAIL_DIMENSION = 512
        const val DISPLAY_DIMENSION = 2160
        const val CONTROLS_VISIBLE_MILLIS = 4_000L
    }
}
