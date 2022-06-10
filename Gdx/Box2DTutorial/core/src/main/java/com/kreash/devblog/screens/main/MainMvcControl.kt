package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.observable.MvcControl

class MainMvcControl(
    private val mvcView: MainMvcView
) : MvcControl,
    MainMvcView.Listener {

    // region MvcControl

    override val screen: Screen
        get() = mvcView.screen

    // endregion MvcControl

    // region MainMvcView.Listener

    // endregion MainMvcView.Listener
}