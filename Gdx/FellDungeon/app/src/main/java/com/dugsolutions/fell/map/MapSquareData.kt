package com.dugsolutions.fell.map

import com.badlogic.gdx.graphics.Color

open class MapSquareData {

    companion object {
        val color = Color.WHITE.toFloatBits()
    }

    // The vertices that make up the mesh to draw this square.
    protected lateinit var vertices: FloatArray

    // how much to subdivide this square;
    private var subdivide: Short = 0

    // A list of elevations relative to the position of this square.
    private var elevations: FloatArray? = null
    protected lateinit var verts: FloatArray
    protected lateinit var indices: ShortArray

    /**
     * Set elevation at the specific subdivision index.
     * x,y starts lower left, going up/right.
     *
     * @param x
     * @param y
     * @param e
     */
    fun addElevation(x: Int, y: Int, e: Float) {
        elevations?.let { elevations -> elevations[index(x, y)] += e }
    }

    private fun index(x: Int, y: Int): Int {
        return y * (subdivide + 1) + x
    }

    /**
     * How much to subdivide the square. A value of 1, means not at all. A value
     * of 2, means 4 sub-squares. 3 means 9, etc.
     *
     * @param d
     */
    fun setSubdivide(d: Int) {
        subdivide = d.toShort()
        val count = (subdivide + 1) * (subdivide + 1)
        elevations = FloatArray(count)
    }

    fun build(
        startX: Float, startY: Float, width: Float, height: Float,
        startU: Float, startV: Float, endU: Float, endV: Float, startZ: Float
    ) {
        if (elevations == null) {
            setSubdivide(1)
        }
        var x: Float
        var u: Float
        val cellSizeX = width / subdivide
        val cellSizeY = height / subdivide
        val usize = endU - startU
        val vsize = endV - startV
        val cellSizeU = usize / subdivide
        val cellSizeV = vsize / subdivide
        val endY = startY + height
        val endX = startX + width
        val color = Color.WHITE.toFloatBits()
        val vertCount = 6 * (subdivide + 1) * (subdivide + 1)
        val indiceCount = 6 * subdivide * subdivide
        verts = FloatArray(vertCount)
        indices = ShortArray(indiceCount)
        var i = 0
        var vi = 0
        var y: Float = startY
        var v: Float = endV
        while (y <= endY) {
            x = startX
            u = startU
            while (x <= endX) {

                // Bottom left vertex
                verts[i++] = x // X
                verts[i++] = y // Y
                verts[i++] = startZ + elevations!![vi] // Z
                verts[i++] = color
                verts[i++] = u // U
                verts[i++] = v // V
                vi++
                x += cellSizeX
                u += cellSizeU
            }
            y += cellSizeY
            v -= cellSizeV
        }

        // Build indices
        i = 0
        var bL: Short = 0
        var tL: Short
        var tR: Short
        var bR: Short
        y = startY
        while (y < endY) {
            x = startX
            while (x < endX) {
                bR = (bL + 1).toShort()
                tL = (bL + subdivide + 1).toShort()
                tR = (tL + 1).toShort()
                // Bottom left
                indices[i++] = bL
                // Top left
                indices[i++] = tL
                // Bottom right
                indices[i++] = tR
                // Bottom Left
                indices[i++] = bL
                // Top right
                indices[i++] = tR
                // Bottom right
                indices[i++] = bR
                x += cellSizeX
                bL++
            }
            y += cellSizeY
            bL++
        }
    }

    open fun setElevations(startX: Float, startY: Float, width: Float, height: Float, startZ: Float) {
        var x: Float
        val cellSizeX = width / subdivide
        val cellSizeY = height / subdivide
        val endY = startY + height
        val endX = startX + width
        var i = 0
        var vi = 0
        var y: Float = startY
        while (y <= endY) {
            x = startX
            while (x <= endX) {
                verts[i + 2] = startZ + elevations!![vi] // Z
                i += 6
                vi++
                x += cellSizeX
            }
            y += cellSizeY
        }
    }

}