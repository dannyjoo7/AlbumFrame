package kr.joolabs.albumframe.presentation.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.application.PhotoAccessStatus
import kr.joolabs.albumframe.domain.PhotoAlbum
import kr.joolabs.albumframe.presentation.HomeFailure
import kr.joolabs.albumframe.presentation.MomentFrameUiState
import kr.joolabs.albumframe.presentation.theme.Accent
import kr.joolabs.albumframe.presentation.theme.Background
import kr.joolabs.albumframe.presentation.theme.Outline
import kr.joolabs.albumframe.presentation.theme.SurfaceRaised
import kr.joolabs.albumframe.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    state: MomentFrameUiState,
    photoAccessStatus: PhotoAccessStatus,
    onRequestPhotoAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    onChooseMorePhotos: () -> Unit,
    onSelectAlbum: (PhotoAlbum) -> Unit,
    onOpenSlideshow: () -> Unit,
    onOpenSettings: () -> Unit,
    loadThumbnail: suspend (String) -> Bitmap?,
) {
    val selected = state.albums.firstOrNull {
        it.id == state.settings.selectedAlbumId
    }
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.slideshow_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = {
            if (photoAccessStatus != PhotoAccessStatus.NONE) {
                Surface(color = Background, tonalElevation = 8.dp) {
                    Button(
                        onClick = onOpenSlideshow,
                        enabled = selected != null && !state.startingSlideshow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        if (state.startingSlideshow) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(
                                if (state.startingSlideshow) {
                                    R.string.loading_photos
                                } else {
                                    R.string.start_slideshow
                                },
                            ),
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        if (photoAccessStatus == PhotoAccessStatus.NONE) {
            PermissionContent(
                modifier = Modifier.padding(contentPadding),
                onRequestPhotoAccess = onRequestPhotoAccess,
                onOpenAppSettings = onOpenAppSettings,
            )
        } else {
            AlbumGrid(
                state = state,
                limited = photoAccessStatus == PhotoAccessStatus.LIMITED,
                selectedAlbumId = selected?.id,
                onRetry = onRetry,
                onChooseMorePhotos = onChooseMorePhotos,
                onOpenAppSettings = onOpenAppSettings,
                onSelectAlbum = onSelectAlbum,
                loadThumbnail = loadThumbnail,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun PermissionContent(
    modifier: Modifier,
    onRequestPhotoAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.permission_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_body),
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPhotoAccess) {
            Text(stringResource(R.string.grant_permission))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenAppSettings) {
            Text(stringResource(R.string.open_app_settings))
        }
    }
}

@Composable
private fun AlbumGrid(
    state: MomentFrameUiState,
    limited: Boolean,
    selectedAlbumId: String?,
    onRetry: () -> Unit,
    onChooseMorePhotos: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSelectAlbum: (PhotoAlbum) -> Unit,
    loadThumbnail: suspend (String) -> Bitmap?,
    modifier: Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = stringResource(R.string.home_subtitle),
                    color = TextSecondary,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(24.dp))
                if (limited) {
                    LimitedAccessBanner(
                        onChooseMorePhotos = onChooseMorePhotos,
                        onOpenAppSettings = onOpenAppSettings,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Text(
                    text = stringResource(R.string.select_album),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        state.failure?.let { failure ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                FailurePanel(failure, onRetry)
            }
        }

        when {
            state.loadingAlbums -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.albums.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyAlbums()
            }
            else -> items(state.albums, key = PhotoAlbum::id) { album ->
                AlbumCard(
                    album = album,
                    limited = limited,
                    selected = album.id == selectedAlbumId,
                    onClick = { onSelectAlbum(album) },
                    loadThumbnail = loadThumbnail,
                )
            }
        }
    }
}

@Composable
private fun LimitedAccessBanner(
    onChooseMorePhotos: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceRaised,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Collections, contentDescription = null, tint = Accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.limited_access_title),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.limited_access_body),
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                FilledTonalButton(onClick = onChooseMorePhotos) {
                    Text(stringResource(R.string.choose_more_photos))
                }
                OutlinedButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.allow_all_photos))
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: PhotoAlbum,
    limited: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    loadThumbnail: suspend (String) -> Bitmap?,
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .then(
                if (selected) Modifier.border(2.dp, Accent, shape) else Modifier,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(album.coverPhotoId, loadThumbnail)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(18.dp),
            ) {
                Text(
                    text = if (limited && album.isAggregate) {
                        stringResource(R.string.accessible_photos)
                    } else {
                        album.name
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.photo_count, album.photoCount),
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AlbumCover(
    photoId: String?,
    loadThumbnail: suspend (String) -> Bitmap?,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoId) {
        value = photoId?.let { loadThumbnail(it) }
    }
    Box(
        modifier = Modifier
            .size(122.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Icon(
                Icons.Outlined.PhotoAlbum,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Image(
                bitmap = requireNotNull(bitmap).asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FailurePanel(failure: HomeFailure, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceRaised) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (failure == HomeFailure.EMPTY_ALBUM) {
                        R.string.empty_album
                    } else {
                        R.string.album_load_failed
                    },
                ),
                modifier = Modifier.weight(1f),
            )
            if (failure == HomeFailure.ALBUMS) {
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun EmptyAlbums() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_albums), fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.no_albums_body),
            color = TextSecondary,
        )
    }
}
