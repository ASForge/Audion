package com.audion.player.gesture

import android.content.Context
import com.audion.player.model.GestureAction

class GesturePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("audion_gesture_prefs", Context.MODE_PRIVATE)

    fun getTwoFingerTapAction(): GestureAction {
        return GestureAction.fromName(
            prefs.getString(KEY_TWO_FINGER_TAP, null),
            GestureAction.PLAY_PAUSE
        )
    }

    fun setTwoFingerTapAction(action: GestureAction) {
        prefs.edit().putString(KEY_TWO_FINGER_TAP, action.name).apply()
    }

    fun getTwoFingerSwipeLeftAction(): GestureAction {
        return GestureAction.fromName(
            prefs.getString(KEY_TWO_FINGER_SWIPE_LEFT, null),
            GestureAction.PREVIOUS_TRACK
        )
    }

    fun setTwoFingerSwipeLeftAction(action: GestureAction) {
        prefs.edit().putString(KEY_TWO_FINGER_SWIPE_LEFT, action.name).apply()
    }

    fun getTwoFingerSwipeRightAction(): GestureAction {
        return GestureAction.fromName(
            prefs.getString(KEY_TWO_FINGER_SWIPE_RIGHT, null),
            GestureAction.NEXT_TRACK
        )
    }

    fun setTwoFingerSwipeRightAction(action: GestureAction) {
        prefs.edit().putString(KEY_TWO_FINGER_SWIPE_RIGHT, action.name).apply()
    }

    fun getDoubleTapPadAction(): GestureAction {
        return GestureAction.fromName(
            prefs.getString(KEY_DOUBLE_TAP_PAD, null),
            GestureAction.READ_NOW_PLAYING
        )
    }

    fun setDoubleTapPadAction(action: GestureAction) {
        prefs.edit().putString(KEY_DOUBLE_TAP_PAD, action.name).apply()
    }

    companion object {
        private const val KEY_TWO_FINGER_TAP = "two_finger_tap"
        private const val KEY_TWO_FINGER_SWIPE_LEFT = "two_finger_swipe_left"
        private const val KEY_TWO_FINGER_SWIPE_RIGHT = "two_finger_swipe_right"
        private const val KEY_DOUBLE_TAP_PAD = "double_tap_pad"
    }
}