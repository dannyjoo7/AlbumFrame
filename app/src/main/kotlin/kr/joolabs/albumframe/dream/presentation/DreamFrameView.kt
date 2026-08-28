package kr.joolabs.albumframe.dream.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import kr.joolabs.albumframe.domain.PhotoFit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 사진 크로스페이드와 시계 오버레이를 소유하는 화면보호기 뷰다. */
internal class DreamFrameView(context: Context) : FrameLayout(context) {
    private val contentContainer = LinearLayout(context)
    private val photoPane = FrameLayout(context)
    private val photoContainer = FrameLayout(context)
    private val cameraPane = FrameLayout(context)
    internal val cameraPreviewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }
    private val firstLayer = PhotoLayer(context)
    private val secondLayer = PhotoLayer(context)
    private val timeView = TextView(context)
    private val dateView = TextView(context)
    private val messageView = TextView(context)
    private val cameraMessageView = TextView(context)
    private val liveBadgeView = createLiveBadge()
    private var activeLayer: PhotoLayer? = null

    init {
        setBackgroundColor(Color.BLACK)
        photoContainer.addView(firstLayer, matchParentLayoutParams())
        photoContainer.addView(secondLayer, matchParentLayoutParams())
        photoPane.addView(photoContainer, matchParentLayoutParams())
        photoPane.addView(createClock(), createClockLayoutParams())
        photoPane.addView(messageView, createMessageLayoutParams())
        cameraPane.addView(cameraPreviewView, matchParentLayoutParams())
        cameraPane.addView(liveBadgeView, createLiveBadgeLayoutParams())
        cameraPane.addView(cameraMessageView, createMessageLayoutParams())
        contentContainer.orientation = LinearLayout.HORIZONTAL
        contentContainer.addView(photoPane, weightedLayoutParams())
        contentContainer.addView(cameraPane, weightedLayoutParams())
        addView(contentContainer, matchParentLayoutParams())
        configureMessage()
        configureCameraMessage()
        setCameraEnabled(false)
    }

    fun showPhoto(bitmap: Bitmap, fit: PhotoFit) {
        messageView.visibility = View.GONE
        val incoming = if (activeLayer === firstLayer) secondLayer else firstLayer
        val outgoing = activeLayer
        incoming.bind(bitmap, fit)
        incoming.alpha = 0f
        incoming.visibility = View.VISIBLE
        photoContainer.bringChildToFront(incoming)
        incoming.animate()
            .alpha(1f)
            .setDuration(CROSSFADE_MILLIS)
            .withEndAction {
                outgoing?.apply {
                    alpha = 0f
                    visibility = View.INVISIBLE
                    clearBitmap()
                }
            }
            .start()
        activeLayer = incoming
    }

    fun showMessage(message: CharSequence) {
        clearPhotos()
        messageView.text = message
        messageView.visibility = View.VISIBLE
    }

    fun updateClock(now: Date = Date()) {
        timeView.text = requireNotNull(TIME_FORMAT.get()).format(now)
        dateView.text = requireNotNull(DATE_FORMAT.get()).format(now)
    }

    fun setCameraEnabled(enabled: Boolean) {
        cameraPane.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    fun showCameraMessage(message: CharSequence) {
        liveBadgeView.visibility = View.GONE
        cameraMessageView.text = message
        cameraMessageView.visibility = View.VISIBLE
    }

    fun clearCameraMessage() {
        cameraMessageView.visibility = View.GONE
        liveBadgeView.visibility = View.VISIBLE
    }

    fun clearPhotos() {
        firstLayer.animate().cancel()
        secondLayer.animate().cancel()
        firstLayer.clearBitmap()
        secondLayer.clearBitmap()
        activeLayer = null
    }

    private fun createClock(): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(
            timeView.apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, CLOCK_TEXT_SIZE_SP)
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                setShadowLayer(CLOCK_SHADOW_RADIUS, 0f, CLOCK_SHADOW_OFFSET, Color.BLACK)
                includeFontPadding = false
                isSingleLine = true
            },
            LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        addView(
            dateView.apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, DATE_TEXT_SIZE_SP)
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setShadowLayer(DATE_SHADOW_RADIUS, 0f, DATE_SHADOW_OFFSET, Color.BLACK)
                includeFontPadding = false
                isSingleLine = true
            },
            LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(CLOCK_DATE_GAP_DP)
            },
        )
    }

    private fun createClockLayoutParams() = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        Gravity.START or Gravity.BOTTOM,
    ).apply {
        marginStart = dp(CLOCK_MARGIN_DP)
        bottomMargin = dp(CLOCK_MARGIN_DP)
    }

    private fun configureMessage() {
        messageView.apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP)
            gravity = Gravity.CENTER
            setPadding(
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
                0,
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
                0,
            )
            visibility = View.GONE
        }
    }

    private fun configureCameraMessage() {
        cameraMessageView.apply {
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP)
            gravity = Gravity.CENTER
            setPadding(
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
                dp(MESSAGE_HORIZONTAL_PADDING_DP),
            )
            visibility = View.GONE
        }
    }

    private fun createLiveBadge(): TextView = TextView(context).apply {
        val label = SpannableString("●  LIVE").also {
            it.setSpan(
                ForegroundColorSpan(Color.rgb(255, 75, 75)),
                0,
                1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        text = label
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, LIVE_TEXT_SIZE_SP)
        setBackgroundColor(Color.argb(150, 0, 0, 0))
        setPadding(dp(12), dp(7), dp(12), dp(7))
    }

    private fun createLiveBadgeLayoutParams() = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        Gravity.START or Gravity.TOP,
    ).apply {
        marginStart = dp(LIVE_MARGIN_DP)
        topMargin = dp(LIVE_MARGIN_DP)
    }

    private fun createMessageLayoutParams() = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
        Gravity.CENTER,
    )

    private fun matchParentLayoutParams() = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
    )

    private fun weightedLayoutParams() = LinearLayout.LayoutParams(
        0,
        LayoutParams.MATCH_PARENT,
        1f,
    )

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics,
        ).toInt()

    private class PhotoLayer(context: Context) : FrameLayout(context) {
        private val background = ImageView(context)
        private val foreground = ImageView(context)
        private var bitmap: Bitmap? = null

        init {
            background.scaleType = ImageView.ScaleType.CENTER_CROP
            background.setColorFilter(BACKDROP_TINT, PorterDuff.Mode.DARKEN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                background.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        BACKDROP_BLUR_RADIUS,
                        BACKDROP_BLUR_RADIUS,
                        Shader.TileMode.CLAMP,
                    ),
                )
            }
            addView(background, matchParentLayoutParams())
            addView(foreground, matchParentLayoutParams())
            visibility = View.INVISIBLE
        }

        fun bind(nextBitmap: Bitmap, fit: PhotoFit) {
            clearBitmap()
            bitmap = nextBitmap
            if (fit == PhotoFit.COVER) {
                background.visibility = View.GONE
                foreground.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                background.visibility = View.VISIBLE
                background.setImageBitmap(nextBitmap)
                foreground.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            foreground.setImageBitmap(nextBitmap)
        }

        fun clearBitmap() {
            background.setImageDrawable(null)
            foreground.setImageDrawable(null)
            bitmap = null
        }

        private fun matchParentLayoutParams() = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )

        private companion object {
            const val BACKDROP_BLUR_RADIUS = 28f
            val BACKDROP_TINT: Int = Color.argb(115, 0, 0, 0)
        }
    }

    private companion object {
        const val CROSSFADE_MILLIS = 500L
        const val CLOCK_TEXT_SIZE_SP = 78f
        const val DATE_TEXT_SIZE_SP = 16f
        const val MESSAGE_TEXT_SIZE_SP = 18f
        const val LIVE_TEXT_SIZE_SP = 12f
        const val CLOCK_SHADOW_RADIUS = 18f
        const val CLOCK_SHADOW_OFFSET = 4f
        const val DATE_SHADOW_RADIUS = 12f
        const val DATE_SHADOW_OFFSET = 3f
        const val CLOCK_MARGIN_DP = 48
        const val CLOCK_DATE_GAP_DP = 8
        const val MESSAGE_HORIZONTAL_PADDING_DP = 32
        const val LIVE_MARGIN_DP = 18
        val TIME_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("HH:mm", Locale.KOREAN)
        }
        val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() =
                SimpleDateFormat("M월 d일 · EEEE", Locale.KOREAN)
        }
    }
}
