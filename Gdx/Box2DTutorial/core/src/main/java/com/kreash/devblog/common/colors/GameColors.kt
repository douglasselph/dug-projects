package com.kreash.devblog.common.colors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20

class GameColors {

    // region public

    val black: Color = Color.BLACK

    fun clear(color: Color) {
        Gdx.gl.glClearColor(color.r, color.g, color.b, color.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    // endregion public

}