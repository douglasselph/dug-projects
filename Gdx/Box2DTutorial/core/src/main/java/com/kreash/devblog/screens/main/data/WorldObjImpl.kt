package com.kreash.devblog.screens.main.data

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.kreash.devblog.common.log.Log

class WorldObjImpl : WorldObj {

    companion object {
        private const val TAG = "WorldObj"
        private val log = Log(TAG)
        const val WORLD_WIDTH = 32f
        const val WORLD_HEIGHT = 24f

        private const val FLOOR_WIDTH = 6f
        private const val FLOOR_HEIGHT = 2f
        private const val FLOOR_XPOS = 0f
        private const val FLOOR_YPOS = -2f

        private const val PLAYER_WIDTH = 1f
        private const val PLAYER_HEIGHT = 1f
        private const val PLAYER_XPOS = 0f
        private const val PLAYER_YPOS = 0f

        private const val MOVABLE_WIDTH = 1f
        private const val MOVABLE_HEIGHT = 1f
        private const val MOVABLE_XPOS = 0f
        private const val MOVABLE_YPOS = 2f
        private const val MOVABLE_VX = 0f
        private const val MOVABLE_VY = 0.25f

        private const val GRAVITY = -9.8f
    }

    private val factory: ObjFactory by lazy { ObjFactory.create(world) }

    private val world: World by lazy {
        val gravity = Vector2(0f, GRAVITY)
        val doSleep = false
        val world = World(gravity, doSleep)
        world
    }

    private val player: Body by lazy {
        factory.box(
            ObjFactory.BoxParams(
                type = ObjFactory.Movable.Mobile,
                x = 2f, y = 10f,
                width = 32f, height = 32f,
                ObjFactory.Material.STEEL
            )
        )
    }

    private val floor: Body by lazy {
        factory.box(
            ObjFactory.BoxParams(
                type = ObjFactory.Movable.Fixed,
                x = 0f, y = 0f,
                width = 64f, height = 32f,
                ObjFactory.Material.STEEL
            )
        )
    }

    // region WorldObj

    override var cameraPosition: Vector2 = Vector2(0f, 0f)
        private set

    override fun create() {
        player
        floor
        cameraPosition = player.position
    }

    override fun render(renderer: Box2DDebugRenderer, camera: OrthographicCamera) {
        renderer.render(world, camera.combined.scl(WorldObj.PPM))
    }

    override fun step(delta: Float) {
        val velocityIteration = 6
        val positionIteration = 2
        world.step(1 / 60f, velocityIteration, positionIteration)
    }

    override fun setPlayerLinearVelocity(xForce: Float) {
        player.setLinearVelocity(xForce, player.linearVelocity.y)
    }

    override fun setPlayerVerticalForce(yForce: Float) {
        player.applyForceToCenter(0f, yForce, true)
    }

    override fun dispose() {
        world.dispose()
    }

    // endregion WorldObj

}