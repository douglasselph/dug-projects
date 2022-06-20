package com.kreash.devblog.screens.main

import com.badlogic.gdx.physics.box2d.World
import com.kreash.devblog.common.observable.MvcView

interface MainMvcView : MvcView<MainMvcView.Listener> {

    interface Listener {
        fun onShown()
        fun onRender(delta: Float): World
        fun onDisposed()
    }

}