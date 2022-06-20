package com.kreash.devblog.screens.preferences

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kreash.devblog.common.display.GameColors
import com.kreash.devblog.common.display.ScreenArrange
import com.kreash.devblog.common.log.Log
import com.kreash.devblog.common.observable.MvcViewImpl
import com.kreash.devblog.common.string.Strings
import com.kreash.devblog.screens.data.GameSkin

class PreferencesMvcViewImpl :
    MvcViewImpl<PreferencesMvcView.Listener>(),
    PreferencesMvcView,
    Screen {

    companion object {
        private const val DEBUG = false
        private const val TAG = "PreferencesMvcView"
        private val log = Log(TAG)
        private const val SCALE = 1.3f
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
        stage.clear()
        stage.addActor(table)
        val titleLabel = Label(Strings.Preferences, skin)
        table.add(titleLabel).colspan(2)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(musicCheckbox).left()
        table.add(musicSlider)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(soundCheckbox).left()
        table.add(soundSlider)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(backButton).colspan(2)
        table
    }
    private val musicCheckbox: CheckBox by lazy {
        val checkbox = CheckBox(Strings.MusicVolume, skin)
        checkbox.addListener {
            val value = checkbox.isChecked
            listeners.forEach { it.onMusicEnabledChanged(value) }
            false
        }
        checkbox
    }
    private val musicSlider: Slider by lazy {
        val slider = Slider(0f, 1f, 0.1f, false, skin)
        slider.addListener {
            val value = slider.value
            listeners.forEach { it.onMusicVolumeAdjusted(value) }
            false
        }
        slider
    }

    private val soundCheckbox: CheckBox by lazy {
        val checkbox = CheckBox(Strings.SoundVolume, skin)
        checkbox.addListener {
            val value = checkbox.isChecked
            listeners.forEach { it.onSoundEnabledChanged(value) }
            false
        }
        checkbox
    }
    private val soundSlider: Slider by lazy {
        val slider = Slider(0f, 1f, 0.1f, false, skin)
        slider.addListener {
            val value = slider.value
            listeners.forEach { it.onSoundVolumeAdjusted(value) }
            false
        }
        slider
    }
    private val backButton: TextButton by lazy {
        val button = TextButton(Strings.Back, skin, "small")
        button.addListener {
            listeners.forEach { it.onBackPressed() }
            false
        }
        button
    }

    // region MvcView

    override val screen: Screen
        get() = this

    // endregion MvcView

    // region PreferencesMvcView

    override var musicEnabled: Boolean
        get() = musicCheckbox.isChecked
        set(value) {
            musicCheckbox.isChecked = value
        }

    override var musicVolume: Float
        get() = musicSlider.value
        set(value) {
            musicSlider.value = value
        }

    override var soundEnabled: Boolean
        get() = soundCheckbox.isChecked
        set(value) {
            soundCheckbox.isChecked = value
        }
    override var soundVolume: Float
        get() = soundSlider.value
        set(value) {
            soundSlider.value = value
        }

    // endregion PreferencesMvcView

    // region Screen

    override fun show() {
        Gdx.input.inputProcessor = stage
        ScreenArrange.fit(camera, table, SCALE)
        listeners.forEach { it.onShown() }
    }

    override fun render(delta: Float) {
        colors.clear(colors.black)
        stage.act(delta)
        stage.draw()
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
        stage.dispose()
        listeners.forEach { it.onDisposed() }
    }

    // endregion Screen

}