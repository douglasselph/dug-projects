package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.data.KeyEvent
import com.kreash.devblog.screens.main.repo.WorldRepo

class MainMvcControl(
    private val mvcView: MainMvcView,
    private val repo: WorldRepo
) : MvcControl,
    MainMvcView.Listener {

    init {
        mvcView.registerListener(this)
    }

    // region MvcControl

    override val screen: Screen
        get() = mvcView.screen

    // endregion MvcControl

    // region MainMvcView.Listener

    override fun onShown() {
    }

    override fun onRender(delta: Float) {
        repo.step(delta)
        mvcView.camera
            .setPosition(repo.cameraPosition)
            .setZoom(repo.cameraZoom)
            .apply()
        mvcView.render(repo.main)
    }

    override fun onDisposed() {
        repo.dispose()
        mvcView.unregisterListeners()
    }

    override fun onKeyDown(key: KeyEvent) {
        repo.onKeyDown(key)
    }

    override fun onKeyUp(key: KeyEvent) {
        repo.onKeyUp(key)
    }

    override fun onMouseDown(screenX: Int, screenY: Int) {
        repo.onMouseDown(screenX, screenY)
    }

    override fun onMouseUp(screenX: Int, screenY: Int) {
        repo.onMouseUp(screenX, screenY)
    }

// endregion MainMvcView.Listener
}