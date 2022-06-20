package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.badlogic.gdx.physics.box2d.World
import com.kreash.devblog.common.observable.MvcControl
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

    override fun onRender(delta: Float): World {
        repo.step(delta)
        return repo.render()
    }

    override fun onDisposed() {
        mvcView.unregisterListeners()
    }

    // endregion MainMvcView.Listener
}