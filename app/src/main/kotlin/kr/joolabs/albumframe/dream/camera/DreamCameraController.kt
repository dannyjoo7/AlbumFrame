package kr.joolabs.albumframe.dream.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kr.joolabs.albumframe.domain.CameraLens

internal class DreamCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onFailure: () -> Unit,
) {
    private var generation = 0
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    fun start(lens: CameraLens) {
        val activeGeneration = ++generation
        stopBoundPreview()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                if (activeGeneration != generation) return@addListener
                runCatching {
                    val activeProvider = providerFuture.get()
                    val selector = activeProvider.firstAvailableSelector(lens)
                        ?: error("No camera is available")
                    val activePreview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    activeProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        activePreview,
                    )
                    provider = activeProvider
                    preview = activePreview
                }.onFailure {
                    if (activeGeneration == generation) onFailure()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun stop() {
        generation++
        stopBoundPreview()
    }

    private fun stopBoundPreview() {
        preview?.let { activePreview ->
            runCatching { provider?.unbind(activePreview) }
        }
        preview = null
        provider = null
    }

    private fun ProcessCameraProvider.firstAvailableSelector(
        lens: CameraLens,
    ): CameraSelector? {
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
}
