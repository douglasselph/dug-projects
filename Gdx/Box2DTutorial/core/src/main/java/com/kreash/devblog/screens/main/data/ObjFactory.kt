package com.kreash.devblog.screens.main.data

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World

interface ObjFactory {

    companion object {
        fun create(world: World): ObjFactory = ObjFactoryImpl(world)
    }

    enum class Movable {
        Fixed,
        Mobile;

        val bodyType: BodyDef.BodyType
            get() {
                return when (this) {
                    Fixed -> BodyDef.BodyType.StaticBody
                    Mobile -> BodyDef.BodyType.DynamicBody
                }
            }
    }

    enum class Material {
        STEEL,
        WOOD,
        RUBBER,
        STONE;

        fun fixture(): FixtureDef {
            val def = FixtureDef()
            when (this) {
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
        val type: Movable,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val material: Material
    )


    data class CircleParams(
        val type: Movable,
        val x: Float,
        val y: Float,
        val radius: Float,
        val rotate: Boolean = false,
        val material: Material
    )

    fun box(params: BoxParams): Body
    fun circle(params: CircleParams): Body

}