package com.kreash.devblog.screens.preferences.cases

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class AppPrefsImpl : AppPrefs {

    companion object {
        private const val NAME = "b2dtut"
    }

    private val prefs: Preferences by lazy { Gdx.app.getPreferences(NAME) }

    // region AppPrefs

    override fun isEnabled(pref: AppPrefs.BooleanPref): Boolean = prefs.getBoolean(pref.key, pref.default)

    override fun setEnabled(pref: AppPrefs.BooleanPref, value: Boolean) {
        prefs.putBoolean(pref.key, value)
        prefs.flush()
    }

    override fun getValue(pref: AppPrefs.FloatPref): Float = prefs.getFloat(pref.key, pref.default)

    override fun setValue(pref: AppPrefs.FloatPref, value: Float) {
        prefs.putFloat(pref.key, value)
        prefs.flush()
    }

    // endregion AppPrefs
}