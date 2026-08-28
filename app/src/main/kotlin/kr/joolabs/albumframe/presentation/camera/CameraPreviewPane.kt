package kr.joolabs.albumframe.presentation.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.domain.CameraLens

@Composable
fun CameraPreviewPane(
    lens: CameraLens,
    cameraAvailable: Boolean,
    cameraAccessGranted: Boolean,
    onRequestCameraAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !cameraAvailable -> CameraMessage(R.string.camera_unavailable)
            !cameraAccessGranted -> CameraMessage(
                message = R.string.camera_permission_required,
                action = onRequestCameraAccess,
            )
            else -> ActiveCameraPreview(lens)
        }
    }
}

@Composable
private fun ActiveCameraPreview(lens: CameraLens) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var failed by remember(lens) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, lens, previewView) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        providerFuture.addListener(
            {
                if (disposed) return@addListener
                runCatching {
                    val provider = providerFuture.get()
                    provider.unbind(preview)
                    val selector = provider.firstAvailableSelector(lens)
                        ?: error("No camera is available")
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                }.onFailure {
                    failed = true
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbind(preview) }
            }
        }
    }

    if (failed) {
        CameraMessage(R.string.camera_open_failed)
    } else {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            LiveBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            )
        }
    }
}

private fun ProcessCameraProvider.firstAvailableSelector(lens: CameraLens): CameraSelector? {
    val preferred = if (lens == CameraLens.FRONT) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
    val fallback = if (lens == CameraLens.FRONT) {
        CameraSelector.DEFAULT_BACK_CAMERA
    } else {
        CameraSelector.DEFAULT_FRONT_CAMERA
    }
    return listOf(preferred, fallback).firstOrNull { selector ->
        runCatching { hasCamera(selector) }.getOrDefault(false)
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.58f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(Color(0xFFFF4B4B), CircleShape),
        )
        Text(
            text = stringResource(R.string.camera_live),
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun CameraMessage(
    message: Int,
    action: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = stringResource(message),
            color = Color.White.copy(alpha = 0.82f),
        )
        if (action != null) {
            Button(onClick = action) {
                Text(stringResource(R.string.grant_camera_permission))
            }
        }
    }
}
