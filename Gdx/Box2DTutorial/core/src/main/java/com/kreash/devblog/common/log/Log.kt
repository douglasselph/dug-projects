package com.kreash.devblog.common.log

import com.badlogic.gdx.Gdx

class Log(private val tag: String) {

    fun msg(msg: String) {
        Gdx.app.log(tag, msg)
    }

}