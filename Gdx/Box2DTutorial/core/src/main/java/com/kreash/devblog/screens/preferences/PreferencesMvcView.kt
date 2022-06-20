package com.kreash.devblog.screens.preferences

import com.kreash.devblog.common.observable.MvcView

interface PreferencesMvcView : MvcView<PreferencesMvcView.Listener> {

    interface Listener {
        fun onMusicEnabledChanged(enabled: Boolean)
        fun onMusicVolumeAdjusted(value: Float)
        fun onSoundEnabledChanged(enabled: Boolean)
        fun onSoundVolumeAdjusted(value: Float)
        fun onBackPressed()
        fun onShown()
        fun onDisposed()
    }

    var musicEnabled: Boolean
    var musicVolume: Float
    var soundEnabled: Boolean
    var soundVolume: Float

}