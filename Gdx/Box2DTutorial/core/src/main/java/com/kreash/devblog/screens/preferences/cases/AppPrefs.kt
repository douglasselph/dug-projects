package com.kreash.devblog.screens.preferences.cases

interface AppPrefs {

    enum class BooleanPref(internal val key: String, internal val default: Boolean = true) {
        Music("music.enabled"),
        Sound("sound.enabled")
    }

    enum class FloatPref(internal val key: String, internal val default: Float = 0.5f) {
        MusicVolume("music.volume"),
        SoundVolume("sound.volume")
    }

    fun isEnabled(pref: BooleanPref): Boolean
    fun setEnabled(pref: BooleanPref, value: Boolean)
    fun getValue(pref: FloatPref): Float
    fun setValue(pref: FloatPref, value: Float)

}