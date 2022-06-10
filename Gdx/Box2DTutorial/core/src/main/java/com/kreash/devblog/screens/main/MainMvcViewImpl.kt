package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.observable.MvcViewImpl

class MainMvcViewImpl :
    MvcViewImpl<MainMvcView.Listener>(),
    MainMvcView,
    Screen {

    // region MvcView

    override val screen: Screen
        get() = this

    // endregion MvcView

    // region Screen

    override fun show() {
    }

    override fun render(delta: Float) {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
    }

    // endregion Screen
}