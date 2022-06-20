package com.kreash.devblog.screens.main.repo

import com.badlogic.gdx.physics.box2d.*

class ObjectFactory(private val world: World) {

    // region public

    enum class Material {
        STEEL,
        WOOD,
        RUBBER,
        STONE;

        fun fixture(): FixtureDef {
            val def = FixtureDef()
            when(this) {
                STEEL -> with(def) {
                    density = 1f
                    friction = 0.3f
                    restitution = 0.1f
                }
                WOOD -> with(def) {
                    density = 0.5f
                    friction = 0.7f
                    restitution = 0.3f
                }
                RUBBER -> with(def) {
                    density = 1f
                    friction = 0f
                    restitution = 1f
                }
                STONE -> with(def) {
                    density = 7f
                    friction = 0.5f
                    restitution = 0.3f
                }
            }
            return def
        }
    }

    data class BoxParams(
        val type: BodyDef.BodyType,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val material: Material
    )

    fun box(params: BoxParams): Body {
        return with(params) {
            val bodyDef = BodyDef()
            bodyDef.type = type
            bodyDef.position.set(x, y)

            val body = world.createBody(bodyDef)

            val shape = PolygonShape()
            shape.setAsBox(width, height)

            val fixtureDef = material.fixture()
            fixtureDef.shape = shape
            body.createFixture(fixtureDef)

            shape.dispose()

            body
        }
    }

    data class CircleParams(
        val type: BodyDef.BodyType,
        val x: Float,
        val y: Float,
        val radius: Float,
        val material: Material
    )

    fun circle(params: BoxParams): Body {
        return with(params) {
            val bodyDef = BodyDef()
            bodyDef.type = type
            bodyDef.position.set(x, y)

            val body = world.createBody(bodyDef)

            val shape = PolygonShape()
            shape.setAsBox(width, height)

            val fixtureDef = material.fixture()
            fixtureDef.shape = shape
            body.createFixture(fixtureDef)

            shape.dispose()

            body
        }
    }

    // endregion public



}