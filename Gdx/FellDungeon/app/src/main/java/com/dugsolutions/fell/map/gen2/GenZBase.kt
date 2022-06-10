package com.dugsolutions.fell.map.gen2

import com.badlogic.gdx.Gdx

/**
 * Base calc value which holds a common entry points which all generators use.
 */
abstract class GenZBase {

    companion object {
        const val TAG = "GenZBase"
    }

    @JvmField
    var icalc: IGenZ? = null
    var count: Int

    constructor() {
        count = 1
    }

    constructor(c: Int) {
        count = c
    }

    fun gen(calc: IGenZ?, startx: Int, starty: Int, endx: Int, endy: Int) {
        icalc = calc
        count--
        Gdx.app.log(TAG, "gen($startx, $starty, $endx, $endy)")
        init(startx, starty, endx, endy)
        for (y in starty..endy) {
            for (x in startx..endx) {
                setZ(x, y)
            }
        }
    }

    protected abstract fun setZ(x: Int, y: Int)
    protected open fun init(startx: Int, starty: Int, endx: Int, endy: Int) {}

}