package com.kreash.devblog.screens.main.data

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer

interface WorldObj {

    companion object {
        const val PPM = 32f
    }

    val cameraPosition: Vector2

    fun create()
    fun render(renderer: Box2DDebugRenderer, camera: OrthographicCamera)

    fun step(delta: Float)
    fun setPlayerLinearVelocity(xForce: Float)
    fun setPlayerVerticalForce(yForce: Float)
    fun dispose()

}