package com.dugsolutions.felldungeon;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector3;

public class MapSquare {
	final static float color = Color.WHITE.toFloatBits();
	// This square is displaying this region in the atlas.
	AtlasRegion region;
	// The vertices that make up the mesh to draw this square.
	float [] vertices;
	// A list of elevations relative to the position of this square.
	ArrayList<Vector3> elevations = new ArrayList<Vector3>();
	
	float[] buildVerts(float startX, float startY, float width, float height,
			float startU, float startV, float endU, float endV, int size) {
		float[] verts;
		int i = 0;
		float x, y;
		float u, v;
		float cellSizeX = width / size;
		float cellSizeY = height / size;
		final int count = 6 * 6 * size * size;
		verts = new float[count];
		float usize = endU - startU;
		float vsize = endV - startV;
		float cellSizeU = usize / size;
		float cellSizeV = vsize / size;

		for (y = startY, v = endV; y <= height; y += cellSizeY, v -= cellSizeV) {
			for (x = startX, u = startU; x <= width; x += cellSizeX, u += cellSizeU) {
				// Bottom left vertex triangle 1
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = color;
				verts[i++] = u; // U
				verts[i++] = v; // V

				// Top left vertex triangle 1
				verts[i++] = x; // X
				verts[i++] = y + cellSizeY; // Y
				verts[i++] = 0; // Z
				verts[i++] = color;
				verts[i++] = u; // U
				verts[i++] = v - cellSizeV; // V

				// Top right vertex triangle 1
				verts[i++] = x + cellSizeX;
				verts[i++] = y + cellSizeY;
				verts[i++] = 0;
				verts[i++] = color;
				verts[i++] = u + cellSizeU;
				verts[i++] = v - cellSizeV;

				// Bottom left vertex triangle 2
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = color;
				verts[i++] = u; // U
				verts[i++] = v; // V

				// Top right vertex triangle 2
				verts[i++] = x + cellSizeX;
				verts[i++] = y + cellSizeY;
				verts[i++] = 0;
				verts[i++] = color;
				verts[i++] = u + cellSizeU;
				verts[i++] = v - cellSizeV;

				// Bottom right vertex triangle 2
				verts[i++] = x + cellSizeX; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = color;
				verts[i++] = u + cellSizeU; // U
				verts[i++] = v; // V
			}
		}
		return verts;
	}
}
