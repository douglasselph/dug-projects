package com.kreash.devblog.screens.menu

import com.kreash.devblog.common.observable.MvcView

interface MenuMvcView : MvcView<MenuMvcView.Listener> {

    interface Listener {
        fun onNewGameClicked()
        fun onPreferencesClicked()
        fun onExitClicked()
        fun onDisposed()
    }

}