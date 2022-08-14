package com.kreash.devblog.screens.main.repo

import com.badlogic.gdx.math.Vector2
import com.kreash.devblog.common.gdx.ScreenTool
import com.kreash.devblog.screens.data.KeyEvent
import com.kreash.devblog.screens.main.data.WorldObj
import kotlin.math.sqrt

class WorldRepo(
    private val createWorld: () -> WorldObj
) {

    companion object {
        private const val FORCE_HORIZONTAL = 1f
        private const val FORCE_VERTICAL = 300f
        private const val ZOOM_RATIO = 10f

        private val triggerRight: Int
            get() = ScreenTool.screenWidth - ScreenTool.screenWidth / 4

        private val triggerLeft: Int
            get() = ScreenTool.screenWidth / 4

        private val triggerUp: Int
            get() = ScreenTool.screenHeight / 4

        private val triggerDown: Int
            get() = ScreenTool.screenHeight - ScreenTool.screenHeight / 4
    }

    private var obj: WorldObj? = null
    private val drag = Dragging()
    private var zoom = 1f

    // region public

    val cameraPosition: Vector2
        get() = main.cameraPosition

    val cameraZoom: Float
        get() = zoom

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
        when (event) {
            KeyEvent.LEFT -> {
                main.setPlayerLinearVelocity(-FORCE_HORIZONTAL)
            }
            KeyEvent.RIGHT -> {
                main.setPlayerLinearVelocity(FORCE_HORIZONTAL)
            }
            KeyEvent.UP -> {
                main.setPlayerVerticalForce(FORCE_VERTICAL)
            }
            KeyEvent.DOWN -> {
                main.setPlayerVerticalForce(-FORCE_VERTICAL)
            }
        }
    }

    fun onKeyUp(event: KeyEvent) {
    }

    fun onMouseDown(screenX: Int, screenY: Int) {
        drag.dragging = true
        drag.dragStart = Vector2(screenX.toFloat(), screenY.toFloat())

        if (screenX < triggerLeft) {
            main.setPlayerLinearVelocity(-FORCE_HORIZONTAL)
        } else if (screenX > triggerRight) {
            main.setPlayerLinearVelocity(FORCE_HORIZONTAL)
        } else {
            drag.dragging = false
        }
        if (screenY < triggerUp) {
            main.setPlayerVerticalForce(FORCE_VERTICAL)
        } else if (screenY > triggerDown) {
            main.setPlayerVerticalForce(-FORCE_VERTICAL)
        } else {
            drag.dragging = false
        }
    }

    fun onMouseUp(screenX: Int, screenY: Int) {
        main.setPlayerLinearVelocity(0f)
        zoom = drag.computeZoom(screenX, screenY)
    }

    // endregion public

    private class Dragging {

        var dragging = false
        var dragStart = Vector2()

        fun computeZoom(endX: Int, endY: Int): Float {
            val centerX = ScreenTool.screenWidth / 2
            val centerY = ScreenTool.screenHeight / 2
            val endDiffX = centerX - endX
            val endDiffY = centerY - endY
            val startDiffX = centerX - dragStart.x
            val startDiffY = centerY - dragStart.y
            val endDist = dist(endDiffX.toFloat(), endDiffY.toFloat())
            val startDist = dist(startDiffX, startDiffY)
            val ratio = endDist / ZOOM_RATIO - startDist / ZOOM_RATIO
            return ratio.toFloat()
        }

        private fun dist(distX: Float, distY: Float): Double =
            sqrt((distX * distX + distY * distY).toDouble())

    }

}