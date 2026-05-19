package com.codewithkael.remotecontrol.utils.prefs

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("remote_control_prefs", Context.MODE_PRIVATE)

    fun shouldShowFullScreenHint(): Boolean {
        return sharedPreferences.getBoolean("show_fullscreen_hint", true)
    }

    fun setDontShowFullScreenHintAgain() {
        sharedPreferences.edit().putBoolean("show_fullscreen_hint", false).apply()
    }
}
