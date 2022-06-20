package com.kreash.devblog.screens.main

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.kreash.devblog.common.display.GameColors
import com.kreash.devblog.common.display.ScreenTool
import com.kreash.devblog.common.log.Log
import com.kreash.devblog.common.observable.MvcViewImpl
import com.kreash.devblog.screens.main.data.WorldObj
import com.kreash.devblog.screens.main.data.WorldObj.Companion.PPM

class MainMvcViewImpl :
    MvcViewImpl<MainMvcView.Listener>(),
    MainMvcView,
    Screen {

    companion object {
        private const val TAG = "MainMvcView"
        private val log = Log(TAG)
    }

    private val colors = GameColors()
    private val camera: OrthographicCamera by lazy { OrthographicCamera() }
    private val renderer: Box2DDebugRenderer by lazy { Box2DDebugRenderer() }

    // region MvcView

    override val screen: Screen
        get() = this

    override fun setCameraPosition(position: Vector2) {
        val cam = camera.position
        cam.x = position.x * PPM
        cam.y = position.y * PPM
        camera.position.set(cam)
        camera.update()
    }

    override fun render(main: WorldObj) {
        main.render(renderer, camera)
    }

    // endregion MvcView

    // region Screen

    override fun show() {
        camera.setToOrtho(false, ScreenTool.screenWidth / 2f, ScreenTool.screenHeight / 2f)
        listeners.forEach { it.onShown() }
    }

    override fun render(delta: Float) {
        colors.clear(colors.black)
        listeners.forEach { it.onRender(delta) }
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width / 2f, height / 2f)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        listeners.forEach { it.onDisposed() }
        renderer.dispose()
    }

    // endregion Screen

}