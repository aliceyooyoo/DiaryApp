package com.example.diaryapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.atan2
import kotlin.math.hypot

class StickerView(context: Context, val imageName: String, val resId: Int) : FrameLayout(context) {

    private val imageView: ImageView = ImageView(context)
    private val deleteBtn: TextView = TextView(context)

    private var dX = 0f
    private var dY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var originalScale = 1f
    private var originalRotation = 0f
    private var isScalingOrRotating = false

    var isSelectedState: Boolean = false
        set(value) {
            field = value
            if (value) {
                deleteBtn.visibility = View.VISIBLE
                setLayerType(LAYER_TYPE_SOFTWARE, null)
                background = object : Drawable() {
                    override fun draw(canvas: Canvas) {
                        val paint = Paint().apply {
                            color = Color.BLUE
                            style = Paint.Style.STROKE
                            strokeWidth = 4f
                            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                        }
                        canvas.drawRect(bounds, paint)
                    }
                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
                }
            } else {
                deleteBtn.visibility = View.GONE
                background = null
            }
        }

    init {
        layoutParams = LayoutParams(300, 300).apply {
            gravity = Gravity.CENTER
        }

        imageView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(20, 20, 20, 20)
        }
        imageView.setImageResource(resId)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        addView(imageView)

        deleteBtn.text = "✕"
        deleteBtn.textSize = 14f
        deleteBtn.setTextColor(Color.WHITE)
        deleteBtn.setBackgroundColor(Color.parseColor("#99000000"))
        deleteBtn.setPadding(12, 4, 12, 4)
        deleteBtn.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }
        deleteBtn.visibility = View.GONE
        deleteBtn.setOnClickListener {
            (parent as? ViewGroup)?.removeView(this)
        }
        addView(deleteBtn)

        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        val parentView = parent as? View ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dX = x - event.rawX
                dY = y - event.rawY
                bringToFront()
                selectThisSticker()
                isScalingOrRotating = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isScalingOrRotating = true
                    originalScale = scaleX
                    originalRotation = rotation
                    lastTouchX = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                    lastTouchY = atan2((event.getY(0) - event.getY(1)).toDouble(), (event.getX(0) - event.getX(1)).toDouble()).toFloat()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2 && isScalingOrRotating) {
                    val newDist = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                    val scale = newDist / lastTouchX
                    val newScale = (originalScale * scale).coerceIn(0.5f, 3.5f)
                    scaleX = newScale
                    scaleY = newScale

                    val angle = atan2((event.getY(0) - event.getY(1)).toDouble(), (event.getX(0) - event.getX(1)).toDouble()).toFloat()
                    val angleDelta = Math.toDegrees((angle - lastTouchY).toDouble()).toFloat()
                    rotation = originalRotation + angleDelta
                } else if (event.pointerCount == 1 && !isScalingOrRotating) {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY

                    newX = newX.coerceIn(0f, (parentView.width - width).toFloat())
                    newY = newY.coerceIn(0f, (parentView.height - height).toFloat())

                    x = newX
                    y = newY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScalingOrRotating = false
            }
        }
        return true
    }

    private fun selectThisSticker() {
        val container = parent as? ViewGroup ?: return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is StickerView) {
                child.isSelectedState = (child == this)
            }
        }
    }
}