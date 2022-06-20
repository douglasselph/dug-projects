package com.kreash.devblog.common.display

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class KeyDetect {

    enum class Key {
        LEFT,
        RIGHT
    }

    fun detect(listener: (key: Key) -> Unit) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            listener(Key.LEFT)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            listener(Key.RIGHT)
        }
    }

}