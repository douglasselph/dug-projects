package com.kreash.devblog.screens.main.repo

import com.badlogic.gdx.math.Vector2
import com.kreash.devblog.common.gdx.ScreenTool
import com.kreash.devblog.screens.data.KeyEvent
import com.kreash.devblog.screens.main.data.WorldObj

class WorldRepo(
    private val createWorld: () -> WorldObj
) {

    companion object {
        private val triggerRight: Int
            get() = ScreenTool.screenWidth - ScreenTool.screenWidth/4

        private val triggerLeft: Int
            get() = ScreenTool.screenWidth/4
    }
    private var obj: WorldObj? = null

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
    }

    fun dispose() {
        main.dispose()
        obj = null
    }

    fun onKeyDown(event: KeyEvent) {
        var horizontalForce = 0f
        if (event == KeyEvent.LEFT) {
            horizontalForce -= 1
        } else if (event == KeyEvent.RIGHT) {
            horizontalForce += 1
        }
        main.setPlayerLinearVelocity(horizontalForce)
    }

    fun onKeyUp(event: KeyEvent) {
        main.setPlayerLinearVelocity(0f)
    }

    fun onMouseDown(screenX: Int, screenY: Int) {
        var horizontalForce = 0f
        if (screenX < triggerLeft) {
            horizontalForce -= 1
        } else if (screenX > triggerRight) {
            horizontalForce += 1
        }
        main.setPlayerLinearVelocity(horizontalForce)
    }

    fun onMouseUp(screenX: Int, screenY: Int) {
        main.setPlayerLinearVelocity(0f)
    }

    // endregion public

}