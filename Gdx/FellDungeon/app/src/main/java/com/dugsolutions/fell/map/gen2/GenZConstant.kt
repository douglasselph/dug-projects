package com.dugsolutions.fell.map.gen2

/**
 * Generate a constant height over all values. Can be sub-classed for generators
 * that have at least a bounds and a height.
 */
open class GenZConstant : GenZBase {
    var height: Float
        protected set

    constructor(height: Float) : super() {
        this.height = height
    }

    constructor(count: Int, height: Float) : super(count) {
        this.height = height
    }

    override fun setZ(x: Int, y: Int) {
        icalc!!.addHeight(x, y, height)
    }
}