package com.kreash.devblog.screens.preferences

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kreash.devblog.common.colors.GameColors
import com.kreash.devblog.common.observable.MvcViewImpl
import com.kreash.devblog.screens.data.GameSkin

class PreferencesMvcViewImpl :
    MvcViewImpl<PreferencesMvcView.Listener>(),
    PreferencesMvcView,
    Screen {

    companion object {
        private const val DEBUG = false
    }

    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private val skin: Skin by lazy { GameSkin.GLASSY.skin }
    private val colors = GameColors()
    private val table: Table by lazy {
        val table = Table()
        table.setFillParent(true)
        table.debug = DEBUG
        stage.addActor(table)
        val titleLabel = Label("Preferences", skin)
        val volumeMusicLabel = Label(null, skin)
        val volumeSoundLabel = Label(null, skin)
        val enabledMusicLabel = Label(null, skin)
        val enabledSoundLabel = Label(null, skin)
        table.add(titleLabel).colspan(2)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(enabledMusicLabel).left()
        table.add(musicCheckbox)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(volumeMusicLabel).left()
        table.add(musicSlider)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(enabledSoundLabel).left()
        table.add(soundCheckbox)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(volumeSoundLabel).left()
        table.add(soundSlider)
        table.row().pad(10f, 0f, 0f, 10f)
        table.add(backButton).colspan(2)
        table
    }
    private val musicCheckbox: CheckBox by lazy {
        val checkbox = CheckBox(null, skin)
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
        val checkbox = CheckBox(null, skin)
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
        val button = TextButton("Back", skin, "small")
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
        table
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
    }

    // endregion Screen

}