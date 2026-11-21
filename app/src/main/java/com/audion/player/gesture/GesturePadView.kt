package com.audion.player.gesture

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.abs

class GesturePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTwoFingerTap()
        fun onTwoFingerSwipeLeft()
        fun onTwoFingerSwipeRight()
        fun onPadDoubleTap()
    }

    private var listener: Listener? = null

    private var isTwoFingerGesture = false
    private var initialCenterX = 0f
    private var initialCenterY = 0f
    private var twoFingerDownTime = 0L

    private var lastSingleTapTime = 0L
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val swipeThreshold = touchSlop * 3
    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun setGestureListener(gestureListener: Listener) {
        this.listener = gestureListener
    }

    override fun performClick(): Boolean {
        super.performClick()
        val now = System.currentTimeMillis()
        if (now - lastSingleTapTime <= doubleTapTimeout) {
            lastSingleTapTime = 0L
            listener?.onPadDoubleTap()
        } else {
            lastSingleTapTime = now
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isTwoFingerGesture = true
                    twoFingerDownTime = System.currentTimeMillis()
                    initialCenterX = (event.getX(0) + event.getX(1)) / 2f
                    initialCenterY = (event.getY(0) + event.getY(1)) / 2f
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTwoFingerGesture && event.pointerCount >= 2) {
                    val currentCenterX = (event.getX(0) + event.getX(1)) / 2f
                    val deltaX = currentCenterX - initialCenterX

                    if (abs(deltaX) >= swipeThreshold) {
                        isTwoFingerGesture = false
                        if (deltaX < 0f) {
                            listener?.onTwoFingerSwipeLeft()
                        } else {
                            listener?.onTwoFingerSwipeRight()
                        }
                        return true
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                if (isTwoFingerGesture) {
                    val elapsed = System.currentTimeMillis() - twoFingerDownTime
                    val currentCenterX = if (event.pointerCount >= 2) {
                        (event.getX(0) + event.getX(1)) / 2f
                    } else {
                        event.x
                    }
                    val currentCenterY = if (event.pointerCount >= 2) {
                        (event.getY(0) + event.getY(1)) / 2f
                    } else {
                        event.y
                    }

                    val dx = abs(currentCenterX - initialCenterX)
                    val dy = abs(currentCenterY - initialCenterY)

                    isTwoFingerGesture = false

                    if (elapsed <= 350L && dx < swipeThreshold && dy < swipeThreshold) {
                        listener?.onTwoFingerTap()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isTwoFingerGesture = false
            }
        }

        return super.onTouchEvent(event)
    }
}