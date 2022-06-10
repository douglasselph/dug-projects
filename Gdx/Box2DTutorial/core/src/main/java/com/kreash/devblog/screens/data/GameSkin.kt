package com.kreash.devblog.screens.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin

enum class GameSkin(private val path: String) {

    BASE("skin/ui/uiskin.json"),
    GLASSY("skin/glassy/glassy-ui.json");

    val skin: Skin by lazy { Skin(Gdx.files.internal(path)) }

}