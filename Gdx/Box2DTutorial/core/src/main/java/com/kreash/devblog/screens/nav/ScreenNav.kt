package com.kreash.devblog.screens.nav

import com.badlogic.gdx.Screen

interface ScreenNav {

    enum class Destination {

        MENU,
        MAIN,
        PREFERENCES

    }

    interface Hook {
        fun toScreen(dest: Destination)
    }

    fun installHook(hook: Hook)
    fun navigateTo(dest: Destination)

}