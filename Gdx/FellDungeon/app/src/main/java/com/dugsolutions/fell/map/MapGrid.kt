package com.dugsolutions.fell.map

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2

class MapGrid {
    private val shaderProgram: ShaderProgram = SpriteBatch.createDefaultShader()
    var numSquaresX = 0
    var numSquaresY = 0

    /**
     * How much to subdivide the square. A value of 1, means not at all. A value
     * of 2, means 4 sub-squares. 3 means 9, etc.
     *
     * @param d
     */
    var subdivide = 1 // How much to subdivide each square.
        set(value) {
            field = value
            subdividePlusOne = field + 1
        }

    var subdividePlusOne = 0

    /**
     * @return real world size of each square on the map.
     */
    var sideSize = 0f

    /**
     * The base Z value for all squares and how much each unit z value stored in a square is in world units.
     */
    var baseZ = 0f
    var startX = 0f
    var startY = 0f

    private lateinit var squares: Array<MapSquareUI?>

    private fun indexSq(x: Int, y: Int): Int {
        return y * numSquaresX + x
    }

    /**
     * Set the region to be displayed at the passed index location.
     * x,y lower left. Indexes determined by size and subdivision.
     * Know what you are doing when using x and y.
     * @param x
     * @param y
     * @param r
     */
    fun setRegion(x: Int, y: Int, r: AtlasRegion) {
        squares[indexSq(x, y)] = MapSquareUI(r)
    }

    /**
     * Set lower left of the map.
     *
     * @param x
     * @param y
     */
    fun setPosition(x: Float, y: Float) {
        startX = x
        startY = y
    }

    /**
     * @return lower left real world corner of map.
     */
    val position: Vector2
        get() = Vector2(startX, startY)

    /**
     * @return real world size of map.
     */
    val mapSize: Vector2
        get() = Vector2(sideSize * numSquaresX, sideSize * numSquaresY)

    /**
     *
     * @return real world size of each sub-division on the map.
     */
    val subdivideSize: Float
        get() = sideSize / subdivide

    /**
     * @return the max number of indices on the X-axis of the map
     */
    val maxIndexX: Int
        get() = subdivide * numSquaresX + 1

    /**
     * @return the max number of indices on the Y-axis of the map
     */
    val maxIndexY: Int
        get() = subdivide * numSquaresY + 1

    /**
     * x,y is lower left running 1 unit per subdivision right and up.
     *
     * @param x
     * @param y
     * @param z elevation value.
     */
    fun addElevation(x: Int, y: Int, z: Float) {
        val sq = getSquare(x, y) ?: return
        val sx = x % subdividePlusOne
        val sy = y % subdividePlusOne
        sq.addElevation(sx, sy, z)
    }

    private fun getSquare(x: Int, y: Int): MapSquareUI? {
        val sx = x / subdividePlusOne
        val sy = y / subdividePlusOne
        return squares[indexSq(sx, sy)]
    }

    /**
     * How many squares in x and y directions, and how much to subdivide each square.
     *
     * @param numX
     * @param numY
     * @param side
     */
    fun setSize(numX: Int, numY: Int, side: Float) {
        numSquaresX = numX
        numSquaresY = numY
        squares = arrayOfNulls(numSquaresX * numSquaresY)
        sideSize = side
    }

    fun build() {
        var x = startX
        var y = startY
        val w = sideSize
        val h = sideSize
        var cnt = 0
        for (sq in squares) {
            sq?.let { sq ->
                sq.setSubdivide(subdivide)
                sq.build(x, y, w, h, baseZ)
                if (++cnt >= numSquaresX) {
                    x = startX
                    y += h
                    cnt = 0
                } else {
                    x += w
                }
            }
        }
    }

    fun setElevations() {
        var x = startX
        var y = startY
        val w = sideSize
        val h = sideSize
        var cnt = 0
        for (sq in squares) {
            sq?.let { sq ->
                sq.setElevations(x, y, w, h, baseZ)
                if (++cnt >= numSquaresX) {
                    x = startX
                    y += h
                    cnt = 0
                } else {
                    x += w
                }
            }
        }
    }

    fun render(projMatrix: Matrix4?) {
        for (sq in squares) {
            sq?.render(shaderProgram, projMatrix)
        }
    }

}