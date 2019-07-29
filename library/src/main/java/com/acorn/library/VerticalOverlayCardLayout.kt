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
    private var layoutTopMaxOffsetY: Float = 0f
    private var layoutBottomMaxOffsetY: Float = 0f

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        orientation = VERTICAL
        context.obtainStyledAttributes(attr, R.styleable.VerticalOverlayCardLayout_Layout).apply {
            setTopMaxOffsetY(getDimension(R.styleable.VerticalOverlayCardLayout_Layout_topMaxOffsetY, 0f))
            setBottomMaxOffsetY(getDimension(R.styleable.VerticalOverlayCardLayout_Layout_bottomMaxOffsetY, 0f))
            recycle()
        }
    }

    fun setTopMaxOffsetY(offsetY: Float) {
        this.layoutTopMaxOffsetY = offsetY
    }

    fun setBottomMaxOffsetY(offsetY: Float) {
        this.layoutBottomMaxOffsetY = offsetY
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
        val offsetY = (event.y - lastY) / 2f
        log("onDragging offsetY:$offsetY,currentY:${event.y},lastY:$lastY")
        lastY = event.y
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val topMargin = (childView.layoutParams as? LayoutParams)?.topMargin ?: continue
            val childLp = (childView.layoutParams as? LayoutParams)
            val topMaxOffsetY: Int =
                0 - (if (childLp == null || childLp.topMaxOffsetY < 0) layoutTopMaxOffsetY.toInt() else childLp.topMaxOffsetY.toInt())
            val bottomMaxOffsetY: Int =
                if (childLp == null || childLp.bottomMaxOffsetY < 0) layoutBottomMaxOffsetY.toInt() else childLp.bottomMaxOffsetY.toInt()
            var offsetMargin = topMargin + offsetY.toInt()
            offsetMargin =
                if (offsetMargin > 0) min(offsetMargin, bottomMaxOffsetY) else max(offsetMargin, topMaxOffsetY)
            val lp = childView.layoutParams as? LayoutParams
            lp?.topMargin = offsetMargin
            childView.layoutParams = lp
            log("onDragging offsetMargin: $offsetMargin,topMax:$topMaxOffsetY,bottomMax:$bottomMaxOffsetY")
        }
    }

    private fun stopDragging(event: MotionEvent) {
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
        //向上拖动时,最大拖动topMargin
        var topMaxOffsetY: Float = 0f
        //向下拖动时,最大拖动topMargin
        var bottomMaxOffsetY: Float = 0f

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.VerticalOverlayCardLayout_Layout)
            topMaxOffsetY = a.getDimension(R.styleable.VerticalOverlayCardLayout_Layout_topMaxOffsetY, -1f)
            bottomMaxOffsetY = a.getDimension(R.styleable.VerticalOverlayCardLayout_Layout_bottomMaxOffsetY, -1f)
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height) {
            topMaxOffsetY = 0f
            bottomMaxOffsetY = 0f
        }

        constructor(width: Int, height: Int, weight: Float) : super(width, height, weight) {
            topMaxOffsetY = 0f
            bottomMaxOffsetY = 0f
        }

        constructor(lp: ViewGroup.LayoutParams) : super(lp)

        constructor(lp: ViewGroup.MarginLayoutParams) : super(lp)

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        constructor(lp: LayoutParams) : super(lp) {
            this.bottomMaxOffsetY = lp.bottomMaxOffsetY
        }
    }
}