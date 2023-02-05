package com.ape.longtimer

import android.app.Activity
import android.view.inputmethod.InputMethodManager


class Utils {
    companion object {
        private const val TIME_FORMAT: String = "%02d:%02d:%02d"

        fun timeStringFromSecs(secs: Int): String {
            val seconds = secs % 60
            val minutes = (secs / 60) % 60
            val hours = secs / (60 * 60)
            return makeTimeString(hours, minutes, seconds)
        }

        fun timeStringFromMillis(ms: Long): String {
            return timeStringFromSecs((ms / 1000).toInt())
        }

        fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String {
            return String.format(TIME_FORMAT, hours, minutes, seconds)
        }

        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager: InputMethodManager = activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            if (inputMethodManager.isAcceptingText()) {
                inputMethodManager.hideSoftInputFromWindow(
                    activity.currentFocus!!.windowToken,
                    0
                )
            }
        }
    }
}