package com.dugsolutions.fell.map;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class MapGrid {

	ShaderProgram shaderProgram;
	int numSquaresX;
	int numSquaresY;
	float sideLen;
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
	
	public void setRegion(int x, int y, AtlasRegion r)
	{
		squares[index(x,y)] = new MapSquareUI(r);
	}

	public void setPosition(float x, float y) {
		startX = x;
		startY = y;
	}

	public void setZ(float z, float scaleZ) {
		baseZ = z;
		elevationScale = scaleZ;
	}

	public void setSize(int numX, int numY, float side) {
		numSquaresX = numX;
		numSquaresY = numY;
		squares = new MapSquareUI[numSquaresX * numSquaresY];
		sideLen = side;
	}

	public void build() {
		float x = startX;
		float y = startY;
		float w = sideLen;
		float h = sideLen;
		int cnt = 0;

		for (MapSquareUI sq : squares) {
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
