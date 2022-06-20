package com.kreash.devblog.screens.main.repo

import com.badlogic.gdx.math.Vector2
import com.kreash.devblog.common.display.KeyDetect
import com.kreash.devblog.screens.main.data.WorldObj

class WorldRepo(
    private val createWorld: () -> WorldObj
) {

    private var obj: WorldObj? = null
    private val detect = KeyDetect()

    // region public

    val cameraPosition: Vector2
        get() = main.cameraPosition

    val main: WorldObj
        get() {
            return obj ?: run {
                val use = createWorld()
                use.create()
                obj = use
                use
            }
        }

    fun step(delta: Float) {
        main.step(delta)
        inputDetect()
    }

    fun dispose() {
        main.dispose()
        obj = null
    }

    // endregion public

    private fun inputDetect() {
        var horizontalForce = 0f
        detect.detect { key ->
            when (key) {
                KeyDetect.Key.LEFT -> horizontalForce -= 1
                KeyDetect.Key.RIGHT -> horizontalForce += 1
            }
        }
        main.setPlayerLinearVelocity(horizontalForce)
    }

}