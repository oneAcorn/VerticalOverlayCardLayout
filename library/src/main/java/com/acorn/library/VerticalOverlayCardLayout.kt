package com.acorn.library

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Created by acorn on 2019-07-26.
 */
class VerticalOverlayCardLayout : LinearLayout {
    private var lastY: Float = 0f
    private val minOffset = -120

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        orientation = VERTICAL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || childCount == 0)
            return false
        when (MotionEventCompat.getActionMasked(event)) {
            MotionEvent.ACTION_DOWN -> startDragging(event)
            MotionEvent.ACTION_MOVE -> onDragging(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopDragging(event)
        }
        return true
    }

    private fun startDragging(event: MotionEvent) {
        lastY = event.y
    }

    private fun onDragging(event: MotionEvent) {
        val offsetY = (event.y - lastY) / 5f
        log("onDragging offsetY:$offsetY,currentY:${event.y},lastY:$lastY")
        lastY = event.y
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val topMargin = (childView.layoutParams as? LayoutParams)?.topMargin ?: continue
            val maxMargin: Int? = (childView.layoutParams as? LayoutParams)?.maxOffsetY?.toInt() ?: continue
//            if (childView.getTag(R.id.overlay_tag_key) == null) {
//                childView.setTag(R.id.overlay_tag_key, topMargin)
//                maxMargin = topMargin
//            }
//            maxMargin = 0
            var offsetMargin = topMargin + offsetY.toInt()
            offsetMargin = if (offsetMargin > 0) min(offsetMargin, maxMargin!!) else max(offsetMargin, minOffset)
            val lp = childView.layoutParams as? LayoutParams
            lp?.topMargin = offsetMargin
            childView.layoutParams = lp
            log("onDragging offsetMargin: $offsetMargin")
        }
    }

    private fun stopDragging(event: MotionEvent) {
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            childView.setTag(R.id.overlay_tag_key, null)
        }
    }

    private fun log(str: String) {
        Log.i("overlayCard", str)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): LinearLayout.LayoutParams {
        if (lp is MarginLayoutParams)
            return LayoutParams(lp)
        return LayoutParams(lp)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun getAccessibilityClassName(): CharSequence {
        return VerticalOverlayCardLayout::class.java.name
    }

    class LayoutParams : LinearLayout.LayoutParams {
        var maxOffsetY: Float = 0f

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.VerticalOverlayCardLayout_Layout)
            maxOffsetY = a.getDimension(R.styleable.VerticalOverlayCardLayout_Layout_maxOffsetY, 0f)
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height) {
            maxOffsetY = 0f
        }

        constructor(width: Int, height: Int, weight: Float) : super(width, height, weight) {
            maxOffsetY = 0f
        }

        constructor(lp: ViewGroup.LayoutParams) : super(lp)

        constructor(lp: ViewGroup.MarginLayoutParams) : super(lp)

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        constructor(lp: LayoutParams) : super(lp) {
            this.maxOffsetY = lp.maxOffsetY
        }
    }
}