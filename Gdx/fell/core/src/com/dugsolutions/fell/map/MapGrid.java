package com.dugsolutions.fell.map;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class MapGrid {

	ShaderProgram shaderProgram;
	int numSquaresX;
	int numSquaresY;
	int subdivide = 1; // How much to subdivide each square.
	float sideLen; // Real world length of each square side.
	float elevationScale = 1;
	float baseZ;
	float startX;
	float startY;
	MapSquareUI[] squares;

	public MapGrid() {
		shaderProgram = SpriteBatch.createDefaultShader();
	}
	
	int index(int x, int y)
	{
		return y * numSquaresX + x;
	}
	
	/**
	 * Set the region to be displayed at the passed index location.
	 * x,y lower left. Indexes determined by size asnd subdivision.
	 * Know what you are doing when using x and y.
	 * @param x
	 * @param y
	 * @param r
	 */
	public void setRegion(int x, int y, AtlasRegion r)
	{
		squares[index(x,y)] = new MapSquareUI(r);
	}

	/**
	 * Set lower left of the map.
	 * 
	 * @param x
	 * @param y
	 */
	public void setPosition(float x, float y) {
		startX = x;
		startY = y;
	}
	
	/**
	 * @return lower left real world corner of map.
	 */
	public Vector2 getPosition()
	{
		return new Vector2(startX, startY);
	}
	
	/**
	 * @return real world size of map.
	 */
	public Vector2 getMapSize()
	{
		return new Vector2(sideLen * numSquaresX, sideLen * numSquaresY);
	}
	
	/**
	 * @return real world size of each square on the map.
	 */
	public float getSideSize()
	{
		return sideLen;
	}
	
	/**
	 * 
	 * @return real world size of each sub-division on the map.
	 */
	public float getSubdivideSize()
	{
		return sideLen / subdivide;
	}
	
	/**
	 * @return  the max number of indices on the X-axis of the map
	 */
	public int getMaxIndexX()
	{
		return subdivide * numSquaresX;
	}
	
	/**
	 * @return  the max number of indices on the Y-axis of the map
	 */
	public int getMaxIndexY()
	{
		return subdivide * numSquaresY;
	}

	/**
	 * Set the base Z value for all squares and how much each unit z value stored in a square is in world units.
	 * @param z
	 * @param scaleZ
	 */
	public void setBaseZ(float z, float scaleZ) {
		baseZ = z;
		elevationScale = scaleZ;
	}

	/**
	 * How many squares in x and y directions, and how much to subdivide each square.
	 * 
	 * @param numX
	 * @param numY
	 * @param side
	 */
	public void setSize(int numX, int numY, float side) {
		numSquaresX = numX;
		numSquaresY = numY;
		squares = new MapSquareUI[numSquaresX * numSquaresY];
		sideLen = side;
	}
	
	/**
	 * How much to subdivide the square. A value of 1, means not at all. A value
	 * of 2, means 4 sub-squares. 3 means 9, etc.
	 * 
	 * @param d
	 */
	public void setSubdivide(int d)
	{
		subdivide = d;
	}

	public void build() {
		float x = startX;
		float y = startY;
		float w = sideLen;
		float h = sideLen;
		int cnt = 0;

		for (MapSquareUI sq : squares) {
			sq.setSubdivide(subdivide);
			sq.build(x, y, w, h, baseZ, elevationScale);

			if (++cnt >= numSquaresX) {
				x = startX;
				y += h;
				cnt = 0;
			} else {
				x += w;
			}
		}
	}

	public void render(Matrix4 projMatrix) {
		for (MapSquareUI sq : squares) {
			sq.render(shaderProgram, projMatrix);
		}
	}

}
