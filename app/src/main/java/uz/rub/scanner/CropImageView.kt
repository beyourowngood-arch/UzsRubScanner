package uz.rub.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private var bitmap: Bitmap? = null
    private val imageRect = RectF()
    private val selection = RectF()
    private var startX = 0f
    private var startY = 0f
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadePaint = Paint().apply { color = 0x88000000.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 3
    }

    fun setImage(image: Bitmap) {
        bitmap = image
        selection.setEmpty()
        updateImageRect()
        invalidate()
    }

    fun hasSelection(): Boolean = selection.width() >= 8f && selection.height() >= 8f

    fun croppedBitmap(): Bitmap? {
        val source = bitmap ?: return null
        if (!hasSelection() || imageRect.isEmpty) return null
        val left = ((selection.left - imageRect.left) / imageRect.width() * source.width)
            .toInt().coerceIn(0, source.width - 1)
        val top = ((selection.top - imageRect.top) / imageRect.height() * source.height)
            .toInt().coerceIn(0, source.height - 1)
        val right = ((selection.right - imageRect.left) / imageRect.width() * source.width)
            .toInt().coerceIn(left + 1, source.width)
        val bottom = ((selection.bottom - imageRect.top) / imageRect.height() * source.height)
            .toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateImageRect()

    private fun updateImageRect() {
        val source = bitmap ?: return
        if (width == 0 || height == 0) return
        val scale = min(width.toFloat() / source.width, height.toFloat() / source.height)
        val shownWidth = source.width * scale
        val shownHeight = source.height * scale
        imageRect.set(
            (width - shownWidth) / 2f,
            (height - shownHeight) / 2f,
            (width + shownWidth) / 2f,
            (height + shownHeight) / 2f,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, null, imageRect, bitmapPaint) }
        if (bitmap != null && hasSelection()) {
            canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, selection.top, shadePaint)
            canvas.drawRect(imageRect.left, selection.bottom, imageRect.right, imageRect.bottom, shadePaint)
            canvas.drawRect(imageRect.left, selection.top, selection.left, selection.bottom, shadePaint)
            canvas.drawRect(selection.right, selection.top, imageRect.right, selection.bottom, shadePaint)
            canvas.drawRect(selection, borderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null || imageRect.isEmpty) return false
        val x = event.x.coerceIn(imageRect.left, imageRect.right)
        val y = event.y.coerceIn(imageRect.top, imageRect.bottom)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                startX = x
                startY = y
                selection.set(x, y, x, y)
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                selection.set(min(startX, x), min(startY, y), max(startX, x), max(startY, y))
                if (event.actionMasked == MotionEvent.ACTION_UP) parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
        invalidate()
        return true
    }
}
