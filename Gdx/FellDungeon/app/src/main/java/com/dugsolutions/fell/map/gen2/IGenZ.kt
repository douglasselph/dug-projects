package com.dugsolutions.fell.map.gen2

import com.badlogic.gdx.math.Vector3

interface IGenZ {
    fun addHeight(x: Int, y: Int, z: Float)
    fun addNormal(x: Int, y: Int, n: Vector3)
    fun genNormal(): Boolean
}