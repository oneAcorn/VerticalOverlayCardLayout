package com.acorn.library

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.support.annotation.FloatRange
import android.support.annotation.RequiresApi
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by acorn on 2019-07-26.
 */
class VerticalOverlayCardLayout : LinearLayout {
    private var lastY: Float = 0f
    private var layoutTopMaxOffsetY: Float = 0f
    private var layoutBottomMaxOffsetY: Float = 0f
    private var mOnCardDragListener: OnCardDragListener? = null
    private var recoverAnimator: ValueAnimator? = null
    private var inertiaAnimator: ValueAnimator? = null
    /**
     * 滑动速度检测类
     */
    private var mVelocityTracker: VelocityTracker? = null
    /**
     * 最小滑动速率
     */
    private var minFlingVelocity: Int = 0
    private var isDraging: Boolean = false

    private val updateListener = ValueAnimator.AnimatorUpdateListener { animation ->
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val lp = childView.layoutParams as? LayoutParams
            lp?.topMargin = (animation.getAnimatedValue("offset$i") as Float).toInt()
            childView.layoutParams = lp
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        orientation = VERTICAL
        context.obtainStyledAttributes(attr, R.styleable.VerticalOverlayCardLayout_Layout).apply {
            setTopMaxOffsetY(getDimension(R.styleable.VerticalOverlayCardLayout_Layout_topMaxOffsetY, 0f))
            setBottomMaxOffsetY(getDimension(R.styleable.VerticalOverlayCardLayout_Layout_bottomMaxOffsetY, 0f))
            recycle()
        }
        minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 8
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
        isDraging = false
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val hitRect = Rect()
            childView.getHitRect(hitRect)
            if (hitRect.contains(event.x.toInt(), event.y.toInt())) {
                isDraging = true
                break
            }
        }
        if (!isDraging)
            return
        lastY = event.y
        //加入速度检测
        mVelocityTracker = VelocityTracker.obtain()
        mVelocityTracker?.addMovement(event)
    }

    private fun onDragging(event: MotionEvent) {
        if (!isDraging) {
            return
        }
        mVelocityTracker?.addMovement(event)
        val offsetY = (event.y - lastY) / 2f
//        log("onDragging offsetY:$offsetY,currentY:${event.y},lastY:$lastY")
        lastY = event.y
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val topMargin = (childView.layoutParams as? LayoutParams)?.topMargin ?: continue
            val topMaxOffsetY: Int = getTopMaxOffsetY(childView).toInt()
            val bottomMaxOffsetY: Int = getBottomMaxOfffsetY(childView).toInt()
            var offsetMargin = topMargin + offsetY.toInt()
            offsetMargin =
                if (offsetMargin > 0) min(offsetMargin, bottomMaxOffsetY) else max(offsetMargin, topMaxOffsetY)
            val lp = childView.layoutParams as? LayoutParams
            lp?.topMargin = offsetMargin
            childView.layoutParams = lp
            mOnCardDragListener?.onDrag(getDragRate(childView))
//            log("onDragging offsetMargin: $offsetMargin,topMax:$topMaxOffsetY,bottomMax:$bottomMaxOffsetY,dragRate:$dragRate")
        }
    }

    private fun stopDragging(event: MotionEvent) {
        if (!isDraging)
            return
        //通过滑动的距离计算出X,Y方向的速度
        mVelocityTracker?.computeCurrentVelocity(1000)
        startInertiaAnimator(mVelocityTracker?.yVelocity ?: 0f)
        releaseDrag()

    }

    private fun releaseDrag() {
        if (mVelocityTracker != null) { //移除速度检测
            mVelocityTracker?.recycle()
            mVelocityTracker = null
        }
    }

    private fun startInertiaAnimator(velocityY: Float) {
        log("startInertiaAnimator velocityY:$velocityY,minVelocity:$minFlingVelocity")
        if (abs(velocityY) > minFlingVelocity) {
            if (null != inertiaAnimator && inertiaAnimator?.isRunning == true)
                inertiaAnimator?.cancel()
            val valueHolders = arrayOfNulls<PropertyValuesHolder>(childCount)
            for (i in 0 until childCount) {
                val childView = getChildAt(i)
                val childDragRate: Float = getDragRate(childView)
                val topMaxOffsetY = getTopMaxOffsetY(childView)
                val bottomMaxOffsetY = getBottomMaxOfffsetY(childView)
                val fromOffset: Float = bottomMaxOffsetY - (bottomMaxOffsetY - topMaxOffsetY) * (1f - childDragRate)
                val inertiaOffset = fromOffset + velocityY / 70f
                val toOffset: Float =
                    if (velocityY > 0) min(inertiaOffset, bottomMaxOffsetY) else max(inertiaOffset, topMaxOffsetY)
                valueHolders[i] = PropertyValuesHolder.ofFloat("offset$i", fromOffset, toOffset)
                log("stopDragging{$i} from:$fromOffset,to:$toOffset")
            }
            inertiaAnimator = ValueAnimator.ofPropertyValuesHolder(*valueHolders).apply {
                duration = (abs(velocityY) / 50).toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        startRecoverAnimator()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }

                })
                addUpdateListener(updateListener)
                start()
            }
        } else {
            startRecoverAnimator()
        }
    }

    private fun startRecoverAnimator() {
        if (null != recoverAnimator && recoverAnimator?.isRunning == true)
            recoverAnimator?.cancel()
        val valueHolders = arrayOfNulls<PropertyValuesHolder>(childCount)
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val childDragRate: Float = getDragRate(childView)
            val topMaxOffsetY = getTopMaxOffsetY(childView)
            val bottomMaxOffsetY = getBottomMaxOfffsetY(childView)
            val fromOffset: Float = bottomMaxOffsetY - (bottomMaxOffsetY - topMaxOffsetY) * (1f - childDragRate)
            val toOffset: Float = if (childDragRate > 0.5f) bottomMaxOffsetY else topMaxOffsetY
            valueHolders[i] = PropertyValuesHolder.ofFloat("offset$i", fromOffset, toOffset)
            log("stopDragging{$i} from:$fromOffset,to:$toOffset")
        }
        recoverAnimator = ValueAnimator.ofPropertyValuesHolder(*valueHolders).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener(updateListener)
            start()
        }
    }

    private fun getTopMaxOffsetY(childView: View): Float {
        val childLp = (childView.layoutParams as? LayoutParams)
        return 0 - (if (childLp == null || childLp.topMaxOffsetY == LayoutParams.INVALID_OFFSET) layoutTopMaxOffsetY else childLp.topMaxOffsetY)
    }

    private fun getBottomMaxOfffsetY(childView: View): Float {
        val childLp = (childView.layoutParams as? LayoutParams)
        return if (childLp == null || childLp.bottomMaxOffsetY == LayoutParams.INVALID_OFFSET) layoutBottomMaxOffsetY else childLp.bottomMaxOffsetY
    }

    private fun getDragRate(childView: View): Float {
        val topMaxOffsetY: Int = getTopMaxOffsetY(childView).toInt()
        val bottomMaxOffsetY: Int = getBottomMaxOfffsetY(childView).toInt()
        val offsetY = (childView.layoutParams as? LayoutParams)?.topMargin?.toFloat() ?: return 0f
        //防止除数为0
        val addValue = if (bottomMaxOffsetY == 0 || bottomMaxOffsetY == topMaxOffsetY) 1 else 0
        return 1f - abs((bottomMaxOffsetY - offsetY + addValue) / (bottomMaxOffsetY - topMaxOffsetY + addValue).toFloat())
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
        companion object {
            //随便定义的无效偏移量(希望这个数值没人用到),当子view未设置相应自定义属性时,使用VerticalOverlayCardLayout的自定义属性
            const val INVALID_OFFSET = -165436f
        }

        //向上拖动时,最大拖动topMargin
        var topMaxOffsetY: Float = 0f
        //向下拖动时,最大拖动topMargin
        var bottomMaxOffsetY: Float = 0f

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.VerticalOverlayCardLayout_Layout)
            topMaxOffsetY = a.getDimension(R.styleable.VerticalOverlayCardLayout_Layout_topMaxOffsetY, INVALID_OFFSET)
            bottomMaxOffsetY =
                a.getDimension(R.styleable.VerticalOverlayCardLayout_Layout_bottomMaxOffsetY, INVALID_OFFSET)
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

    fun setOnCardDragListener(onCardDragListener: OnCardDragListener) {
        this.mOnCardDragListener = onCardDragListener
    }

    interface OnCardDragListener {
        fun onDrag(@FloatRange(from = 0.00, to = 1.00) rate: Float)
    }
}