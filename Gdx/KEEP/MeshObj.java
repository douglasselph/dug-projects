package com.dugsolutions.felldungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.math.Matrix4;

public class MeshObj {

	Mesh mesh;
	ShaderProgram shaderProgram;
	Texture texture;
	
	public MeshObj(AtlasRegion region, float x, float y, float width, float height, int size) {
		texture = region.getTexture();

		float verts[] = buildVerts(x, y, width, height, region.getU(),
				region.getV(), region.getU2(), region.getV2(), size);

		// Create a mesh out of two triangles rendered clockwise without indices
		mesh = new Mesh(true, verts.length / 5, 0, new VertexAttribute(
				VertexAttributes.Usage.Position, 3,
				ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
				VertexAttributes.Usage.ColorPacked, 4,
				ShaderProgram.COLOR_ATTRIBUTE), new VertexAttribute(
				VertexAttributes.Usage.TextureCoordinates, 2,
				ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);

		shaderProgram = SpriteBatch.createDefaultShader();

		String log = shaderProgram.getLog();
		if (!shaderProgram.isCompiled())
			throw new GdxRuntimeException(log);
		if (log != null && log.length() != 0)
			System.out.println("Shader Log: " + log);
	}

	public void render(Matrix4 projMatrix) {
		texture.bind();
		shaderProgram.begin();
		shaderProgram.setUniformMatrix("u_projTrans", projMatrix);
		shaderProgram.setUniformi("u_texture", 0);
		mesh.render(shaderProgram, GL20.GL_TRIANGLES);
		shaderProgram.end();

	}

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
		float color = Color.WHITE.toFloatBits();

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
