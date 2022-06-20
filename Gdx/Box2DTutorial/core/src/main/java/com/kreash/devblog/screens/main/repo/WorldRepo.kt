package com.kreash.devblog.screens.main.repo

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.kreash.devblog.common.log.Log

class WorldRepo {

    companion object {
        private const val TAG = "WorldRepo"
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

        private const val GRAVITY = -10f
    }

    private val world: World by lazy {
        val gravity = Vector2(0f, GRAVITY)
        val doSleep = true
        val world = World(gravity, doSleep)
        world
    }

    private val factory: ObjectFactory by lazy { ObjectFactory(world) }

    private val floor: Body by lazy {
        factory.box(
            ObjectFactory.BoxParams(
                type = BodyDef.BodyType.StaticBody,
                x = FLOOR_XPOS, y = FLOOR_YPOS,
                width = FLOOR_WIDTH, height = FLOOR_HEIGHT,
                ObjectFactory.Material.STEEL
            )
        )
    }

    private val player: Body by lazy {
        factory.box(
            ObjectFactory.BoxParams(
                type = BodyDef.BodyType.DynamicBody,
                x = PLAYER_XPOS, y = PLAYER_YPOS,
                width = PLAYER_WIDTH, height = PLAYER_HEIGHT,
                ObjectFactory.Material.STEEL
            )
        )
    }

    private val movable1: Body by lazy {
        val obj = factory.box(
            ObjectFactory.BoxParams(
                BodyDef.BodyType.KinematicBody,
                x = MOVABLE_XPOS, y = MOVABLE_YPOS,
                width = MOVABLE_WIDTH, height = MOVABLE_HEIGHT,
                ObjectFactory.Material.STEEL
            )
        )
        obj.setLinearVelocity(MOVABLE_VX, MOVABLE_VY)
        obj
    }

    private val movable2: Body by lazy {
        val obj = factory.box(
            ObjectFactory.BoxParams(
                BodyDef.BodyType.KinematicBody,
                x = MOVABLE_XPOS, y = MOVABLE_YPOS,
                width = MOVABLE_WIDTH, height = MOVABLE_HEIGHT,
                ObjectFactory.Material.STEEL
            )
        )
        obj.setLinearVelocity(MOVABLE_VY, MOVABLE_VX)
        obj
    }

    // region public

    fun render(): World {
        floor
        player
        movable1
        movable2
        return world
    }

    fun step(delta: Float) {
        val velocityIteration = 3
        val positionIteration = 3
        world.step(delta, velocityIteration, positionIteration)
    }

    // endregion public

}