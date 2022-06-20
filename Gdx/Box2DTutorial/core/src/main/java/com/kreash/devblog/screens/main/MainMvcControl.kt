package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.main.data.WorldObj
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
        mvcView.setCameraPosition(repo.cameraPosition)
        mvcView.render(repo.main)
    }

    override fun onDisposed() {
        repo.dispose()
        mvcView.unregisterListeners()
    }

    // endregion MainMvcView.Listener
}