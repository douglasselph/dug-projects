package com.dugsolutions.fell.map.gen2

import com.badlogic.gdx.math.Vector3
import kotlin.math.atan

/**
 * A center point where the height is at max.
 *
 * Then a slope defines a parabolic slope outward toward the edge, max circle
 * radius, where the value is zero.
 */
open class GenZMound : GenZCone {
    protected var mPA = 0f

    constructor(height: Float) : super(height)
    constructor(count: Int, height: Float) : super(count, height)

    override fun setZ(x: Int, y: Int) {
        val dX = x - fCenterX
        val dY = y - fCenterY
        val dXSquared = dX * dX
        val dYSquared = dY * dY
        val height: Float
        val dist = Math.sqrt((dXSquared + dYSquared).toDouble()).toFloat()
        if (mIsCircle) {
            height = -mPA * dist * dist + super.height
        } else {
            val maxDist: Float
            if (dX != 0f) {
                var angleT = atan((dY / dX).toDouble()).toFloat()
                if (dX < 0) {
                    angleT += Math.PI.toFloat()
                }
                maxDist = getDistOnEllipse(angleT.toDouble()).toFloat()
            } else {
                maxDist = fB
            }
            // Parabola along the line in the ellipsoid of interest.
            height = -(super.height / (maxDist * maxDist)) * dist * dist
        }
        if (height > 0) {
            val ic = icalc ?: return
            ic.addHeight(x, y, height)
            if (ic.genNormal()) {
                val normal = Vector3(dX, dY, -height)
                normal.nor()
                ic.addNormal(x, y, normal)
            }
        }
    }

    override fun init(startx: Int, starty: Int, endx: Int, endy: Int) {
        super.init(startx, starty, endx, endy)
        mPA = height / (fA * fA)
    }
}