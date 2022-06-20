package com.kreash.devblog.screens.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kreash.devblog.common.display.GameColors
import com.kreash.devblog.common.display.ScreenTool
import com.kreash.devblog.common.log.Log
import com.kreash.devblog.common.observable.MvcViewImpl
import com.kreash.devblog.common.string.Strings
import com.kreash.devblog.screens.data.GameSkin

class MenuMvcViewImpl :
    MvcViewImpl<MenuMvcView.Listener>(),
    MenuMvcView,
    Screen {

    companion object {
        private const val DEBUG = false
        private const val TAG = "MenuMvcView"
        private val log = Log(TAG)
        private const val SCALE = 1.4f
    }

    private val camera: OrthographicCamera by lazy { OrthographicCamera() }
    private val viewport: ScreenViewport by lazy { ScreenViewport(camera) }
    private val stage: Stage by lazy { Stage(viewport) }
    private val skin: Skin by lazy { GameSkin.GLASSY.skin }
    private val colors = GameColors()
    private val table: Table by lazy {
        val table = Table()
        table.setFillParent(true)
        table.debug = DEBUG
        stage.addActor(table)
        val newGame = TextButton(Strings.NewGame, skin)
        newGame.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                listeners.forEach { it.onNewGameClicked() }
            }
        })
        val preferences = TextButton(Strings.Preferences, skin)
        preferences.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                listeners.forEach { it.onPreferencesClicked() }
            }
        })
        val exit = TextButton(Strings.Exit, skin)
        exit.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                listeners.forEach { it.onExitClicked() }
            }
        })
        table.add(newGame).fillX().uniformX()
        table.row().pad(10f, 0f, 10f, 0f)
        table.add(preferences).fillX().uniformX()
        table.row();
        table.add(exit).fillX().uniformX()
        table
    }

    // region MvcView

    override val screen: Screen
        get() = this

    // endregion MvcView

    // region Screen

    override fun show() {
        Gdx.input.inputProcessor = stage
        ScreenTool.fit(camera, table, SCALE)
    }

    override fun render(delta: Float) {
        colors.clear(colors.black)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose() // Not strictly necessary as it will be disposed by the Asset manager
        listeners.forEach { it.onDisposed() }
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    // endregion Screen

}