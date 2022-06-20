package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.utils.viewport.FillViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.kreash.devblog.common.display.GameColors
import com.kreash.devblog.common.log.Log
import com.kreash.devblog.common.observable.MvcViewImpl
import com.kreash.devblog.screens.main.repo.WorldRepo
import com.kreash.devblog.screens.main.repo.WorldRepo.Companion.WORLD_HEIGHT

class MainMvcViewImpl :
    MvcViewImpl<MainMvcView.Listener>(),
    MainMvcView,
    Screen {

    companion object {
        private const val WORLD_WIDTH = WorldRepo.WORLD_WIDTH
        private const val WORLD_HEIGHT = WorldRepo.WORLD_HEIGHT
        private const val TAG = "MainMvcView"
        private val log = Log(TAG)
    }

    private val colors = GameColors()
    private val camera: OrthographicCamera by lazy { OrthographicCamera(WORLD_WIDTH, WORLD_HEIGHT) }
    private val viewport: Viewport by lazy { FillViewport(WORLD_WIDTH, WORLD_HEIGHT, camera) }
    private val renderer: Box2DDebugRenderer by lazy {
        Box2DDebugRenderer(
            true,
            true,
            true,
            true,
            true,
            true
        )
    }

    // region MvcView

    override val screen: Screen
        get() = this

    // endregion MvcView

    // region Screen

    override fun show() {
//        viewport.apply()
//        center()
        listeners.forEach { it.onShown() }
    }

    override fun render(delta: Float) {
        colors.clear(colors.black)
        listeners.forEach {
            val world = it.onRender(delta)
            renderer.render(world, camera.combined)
        }
    }

    override fun resize(width: Int, height: Int) {
//        viewport.update(width, height)
//        center()
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        listeners.forEach { it.onDisposed() }
    }

    // endregion Screen

    private fun center() {
        val centerX = camera.viewportWidth / 2f
        val centerY = camera.viewportHeight / 2f
        camera.position.set(centerX, centerY, 0f)
    }
}