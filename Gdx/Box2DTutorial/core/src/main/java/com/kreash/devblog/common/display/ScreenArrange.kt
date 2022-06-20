package com.kreash.devblog.common.display

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.Table

object ScreenArrange {

    fun fit(camera: OrthographicCamera, table: Table, scale: Float = 1.3f) {
        camera.zoom = table.prefHeight / table.stage.viewport.screenHeight * scale
    }

    val screenWidth: Int
        get() = Gdx.graphics.width

    val screenHeight: Int
        get() = Gdx.graphics.height

}