package com.kreash.devblog.screens.preferences

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.nav.SystemNav
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.nav.ScreenNav
import com.kreash.devblog.screens.preferences.cases.AppPrefs
import com.kreash.devblog.screens.preferences.cases.AppPrefsImpl

class PreferencesMvcControl(
    private val mvcView: PreferencesMvcView,
    private val prefs: AppPrefs,
    private val screenNav: ScreenNav
) : MvcControl,
    PreferencesMvcView.Listener {

    // region MvcControl

    override val screen: Screen
        get() = mvcView.screen

    // endregion MvcControl

    // region PreferencesMvcView.Listener

    override fun onShown() {
        mvcView.musicEnabled = prefs.isEnabled(AppPrefs.BooleanPref.Music)
        mvcView.musicVolume = prefs.getValue(AppPrefs.FloatPref.MusicVolume)
        mvcView.soundEnabled = prefs.isEnabled(AppPrefs.BooleanPref.Sound)
        mvcView.soundVolume = prefs.getValue(AppPrefs.FloatPref.SoundVolume)
    }

    override fun onMusicEnabledChanged(enabled: Boolean) {
        prefs.setEnabled(AppPrefs.BooleanPref.Music, enabled)
    }

    override fun onMusicVolumeAdjusted(value: Float) {
        prefs.setValue(AppPrefs.FloatPref.MusicVolume, value)
    }

    override fun onSoundEnabledChanged(enabled: Boolean) {
        prefs.setEnabled(AppPrefs.BooleanPref.Sound, enabled)
    }

    override fun onSoundVolumeAdjusted(value: Float) {
        prefs.setValue(AppPrefs.FloatPref.SoundVolume, value)
    }

    override fun onBackPressed() {
        screenNav.navigateTo(ScreenNav.Destination.MENU)
    }

    // endregion PreferencesMvcView.Listener

}