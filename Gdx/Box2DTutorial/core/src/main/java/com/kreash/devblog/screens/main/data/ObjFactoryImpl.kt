package com.kreash.devblog.screens.main.data

import com.badlogic.gdx.physics.box2d.*
import com.kreash.devblog.screens.main.data.WorldObj.Companion.PPM

class ObjFactoryImpl(private val world: World) : ObjFactory {

    // region ObjFactory

    override fun box(params: ObjFactory.BoxParams): Body {
        return with(params) {
            val bodyDef = BodyDef()
            bodyDef.type = type.bodyType
            bodyDef.position.set(x / PPM, y / PPM)
            bodyDef.fixedRotation = true

            val body = world.createBody(bodyDef)

            val shape = PolygonShape()
            shape.setAsBox(width / 2f / PPM, height / 2f / PPM)

            val fixtureDef = material.fixture()
            fixtureDef.shape = shape
            body.createFixture(fixtureDef)

            shape.dispose()

            body
        }
    }

    override fun circle(params: ObjFactory.CircleParams): Body {
        return with(params) {
            val bodyDef = BodyDef()
            bodyDef.type = type.bodyType
            bodyDef.position.set(x, y)
            bodyDef.fixedRotation = !rotate

            val body = world.createBody(bodyDef)

            val shape = CircleShape()
            shape.radius = radius / 2

            val fixtureDef = material.fixture()
            fixtureDef.shape = shape
            body.createFixture(fixtureDef)

            shape.dispose()

            body
        }
    }

    // endregion ObjFactory

}