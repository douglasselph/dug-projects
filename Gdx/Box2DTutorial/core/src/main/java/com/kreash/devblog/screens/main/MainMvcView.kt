package com.kreash.devblog.screens.main

import com.badlogic.gdx.math.Vector2
import com.kreash.devblog.common.observable.MvcView
import com.kreash.devblog.screens.data.KeyEvent
import com.kreash.devblog.screens.main.data.WorldObj

interface MainMvcView : MvcView<MainMvcView.Listener> {

    interface Listener {
        fun onShown()
        fun onRender(delta: Float)
        fun onDisposed()
        fun onKeyDown(key: KeyEvent)
        fun onKeyUp(key: KeyEvent)
        fun onMouseDown(screenX: Int, screenY: Int)
        fun onMouseUp(screenX: Int, screenY: Int)
    }

    interface Camera {
        fun setPosition(position: Vector2): Camera
        fun setZoom(zoom: Float): Camera
        fun apply()
    }

    val camera: Camera
    fun render(main: WorldObj)

}