package com.dugsolutions.fell.map.gen2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Vector3
import com.dugsolutions.fell.map.MapGrid
import java.util.*

/**
 * Given a MapGrid, apply a host of z generators across it.
 *
 * @author dug
 */
class GenZMap(var map: MapGrid) {

    companion object {
        const val LOG = true
        const val TAG = "GenZMap"
    }

    private var generators = ArrayList<GenZBase>()
    private var random = Random()
    private var sizeLo = 0
    private var sizeHi: Int
    private var calc = MyCalcResult()

    init {
        sizeHi = if (map.maxIndexX < map.maxIndexY) map.maxIndexX else map.maxIndexY
        sizeLo = if (sizeHi > 2) {
            2
        } else {
            1
        }
    }

    inner class MyCalcResult : IGenZ {
        override fun addHeight(x: Int, y: Int, z: Float) {
            if (LOG) {
                Gdx.app.log(TAG, "addHeight($x, $y, $z)")
            }
            map.addElevation(x, y, z)
        }

        override fun addNormal(x: Int, y: Int, n: Vector3) {}
        override fun genNormal(): Boolean {
            return false
        }
    }


    fun addGenerator(gen: GenZBase) {
        generators.add(gen)
    }

    fun setRandomSeed(s: Long) {
        random = Random(s)
    }

    /**
     * Set the range the bounding box for each gen can be. The X and Y sizes
     * will randomly be between the indicated range.
     *
     * @param lo
     * : lo size in indices
     * @param hi
     * : hi size in indices
     */
    fun setGenSize(lo: Int, hi: Int) {
        sizeLo = lo
        sizeHi = hi
    }

    /**
     * Go through each generator the indicated number of times at random
     * locations and sizes on the map.
     */
    fun run() {
        var gen: GenZBase
        // Subdivision indices
        val maxXs = map.maxIndexX
        val maxYs = map.maxIndexY
        val sizeLen = sizeHi - sizeLo + 1
        var xsStart: Int // subdivision index x
        var ysStart: Int // subdivision index y
        var xsSize: Int // subdivision size x
        var ysSize: Int // subdivision size y;
        while (nextGen.also { gen = it!! } != null) {
            xsSize = random.nextInt(sizeLen) + sizeLo
            ysSize = random.nextInt(sizeLen) + sizeLo
            xsStart = random.nextInt(maxXs + 1 - xsSize)
            ysStart = random.nextInt(maxYs + 1 - ysSize)
            gen.gen(
                calc, xsStart, ysStart, xsStart + xsSize - 1, ysStart
                        + ysSize - 1
            )
            if (gen.count <= 0) {
                generators.remove(gen)
            }
        }
        map.setElevations()
    }

    private val nextGen: GenZBase?
        get() {
            var gen: GenZBase
            if (generators.size == 0) {
                return null
            }
            val idx = random.nextInt(generators.size)
            for (i in generators.indices) {
                gen = generators[(idx + i) % generators.size]
                if (gen.count > 0) {
                    return gen
                }
            }
            return null
        }

}