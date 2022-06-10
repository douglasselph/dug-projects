package com.dugsolutions.fell.map.gen2

import com.badlogic.gdx.math.Vector3
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A Cone Generator.
 *
 * There is a defined boundary which indicates either a circular area or an
 * elliptical area. If it is a circle, the center of the circle is the height.
 * The edge of the circle is zero. If it is an ellipse, the two foci of the
 * ellipse and the line connecting them, is the height. The distance from the
 * foci determines what percentage of the height to use until the edge of the
 * ellipse is reached which is zero.
 */
open class GenZCone : GenZConstant {
    @JvmField
		protected var fA = 0f
    @JvmField
		protected var fB = 0f
    private var fAB = 0f
    @JvmField
		protected var fCenterX // Center of circle and ellipse
            = 0f
    @JvmField
		protected var fCenterY = 0f
    @JvmField
		protected var mIsCircle // Otherwise ellipse which is more complicated
            = false

    constructor(count: Int, height: Float) : super(count, height)
    constructor(height: Float) : super(height)

    override fun init(startx: Int, starty: Int, endx: Int, endy: Int) {
        val sizex = endx - startx
        val sizey = endy - starty
        fCenterX = ((startx + endx) / 2).toFloat()
        fCenterY = ((starty + endy) / 2).toFloat()
        mIsCircle = sizex == sizey
        fA = sizex / 2f
        fB = sizey / 2f
        fAB = fA * fB
    }

    override fun setZ(x: Int, y: Int) {
        val height: Float
        val percent: Float
        val dX = x - fCenterX
        val dY = y - fCenterY
        val dXSquared = dX * dX
        val dYSquared = dY * dY
        val dist = sqrt((dXSquared + dYSquared).toDouble()).toFloat()
        val maxDist: Float
        if (mIsCircle) {
            maxDist = fA
        } else {
            if (dX != 0f) {
                var angleT = atan((dY / dX).toDouble()).toFloat()
                if (dX < 0) {
                    angleT += Math.PI.toFloat()
                }
                maxDist = getDistOnEllipse(angleT.toDouble()).toFloat()
            } else {
                maxDist = fB
            }
        }
        if (dist <= maxDist) {
            percent = 1 - dist / maxDist
            height = percent * super.height
            val ic = icalc ?: return
            ic.addHeight(x, y, height)
            if (ic.genNormal() && height > 0) {
                val normal = Vector3(dX, dY, height)
                normal.nor()
                ic.addNormal(x, y, normal)
            }
        }
    }

    /**
     * Return the distance from the center the point on the ellipse would be at
     * the given angle.
     *
     * @param angleT
     * @return
     */
    protected fun getDistOnEllipse(angleT: Double): Double {
        val termB = fB * cos(angleT)
        val termA = fA * sin(angleT)
        return fAB / sqrt(termB * termB + termA * termA)
    }
}