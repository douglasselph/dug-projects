package com.dugsolutions.fell.map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class MapSquareData {
	final static float color = Color.WHITE.toFloatBits();

	// The vertices that make up the mesh to draw this square.
	protected float[] vertices;
	// how much to subdivide this square;
	protected short subdivide;
	// A list of elevations relative to the position of this square.
	protected float[] elevations;

	protected float[] verts;
	protected short[] indices;

	/**
	 * Set elevation at the specific subdivision index.
	 * x,y starts lower left, going up/right.
	 * 
	 * @param x
	 * @param y
	 * @param e
	 */
	public void addElevation(int x, int y, float e)
	{
		elevations[index(x, y)] += e;
	}
	
	int index(int x, int y)
	{
		return y * (subdivide+1) + x;
	}

	/**
	 * How much to subdivide the square. A value of 1, means not at all. A value
	 * of 2, means 4 sub-squares. 3 means 9, etc.
	 * 
	 * @param d
	 */
	void setSubdivide(int d) {
		subdivide = (short) d;
		int count = (subdivide + 1) * (subdivide + 1);
		elevations = new float[count];
	}

	void build(float startX, float startY, float width, float height,
			float startU, float startV, float endU, float endV, float startZ) {

		if (elevations == null) {
			setSubdivide(1);
		}
		int i;
		int vi;
		float x, y;
		float u, v;
		float cellSizeX = width / subdivide;
		float cellSizeY = height / subdivide;
		float usize = endU - startU;
		float vsize = endV - startV;
		float cellSizeU = usize / subdivide;
		float cellSizeV = vsize / subdivide;
		float endY = startY + height;
		float endX = startX + width;
		float color = Color.WHITE.toFloatBits();
		final int vertCount = 6 * (subdivide + 1) * (subdivide + 1);
		final int indiceCount = 6 * subdivide * subdivide;
		verts = new float[vertCount];
		indices = new short[indiceCount];

		i = 0;
		vi = 0;
		for (y = startY, v = endV; y <= endY; y += cellSizeY, v -= cellSizeV) {
			for (x = startX, u = startU; x <= endX; x += cellSizeX, u += cellSizeU) {
				// Bottom left vertex
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = startZ + elevations[vi]; // Z
				verts[i++] = color;
				verts[i++] = u; // U
				verts[i++] = v; // V
				vi++;
			}
		}

		// Build indices
		i = 0;
		short bL = 0;
		short tL;
		short tR;
		short bR;

		for (y = startY; y < endY; y += cellSizeY, bL++) {
			for (x = startX; x < endX; x += cellSizeX, bL++) {
				bR = (short) (bL + 1);
				tL = (short) (bL + subdivide + 1);
				tR = (short) (tL + 1);
				// Bottom left
				indices[i++] = bL;
				// Top left
				indices[i++] = tL;
				// Bottom right
				indices[i++] = tR;
				// Bottom Left
				indices[i++] = bL;
				// Top right
				indices[i++] = tR;
				// Bottom right
				indices[i++] = bR;
			}
		}
	}
	
	public void setElevations(float startX, float startY, float width, float height, float startZ)
	{
		float x, y;
		float cellSizeX = width / subdivide;
		float cellSizeY = height / subdivide;
		float endY = startY + height;
		float endX = startX + width;
		int i = 0;
		int vi = 0;
		for (y = startY; y <= endY; y += cellSizeY) {
			for (x = startX; x <= endX; x += cellSizeX) {
				verts[i + 2] = startZ + elevations[vi]; // Z
				i += 6;
				vi++;
			}
		}
	}
}
