package com.dugsolutions.fell;

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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class MeshObj2 {

	Mesh mesh;
	ShaderProgram shaderProgram;
	Texture texture;
	float[] verts;
	short[] indices;

	public MeshObj2(AtlasRegion region, float x, float y, float width,
			float height, int size) {
		texture = region.getTexture();

		build(x, y, width, height, region.getU(), region.getV(),
				region.getU2(), region.getV2(), size);

		// Create a mesh out of two triangles rendered clockwise without indices
		mesh = new Mesh(true, verts.length / 6, indices.length,
				new VertexAttribute(VertexAttributes.Usage.Position, 3,
						ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
						VertexAttributes.Usage.ColorPacked, 4,
						ShaderProgram.COLOR_ATTRIBUTE), new VertexAttribute(
						VertexAttributes.Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);
		mesh.setIndices(indices);

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

	void build(float startX, float startY, float width, float height,
			float startU, float startV, float endU, float endV, int size) {
		int i = 0;
		float x, y;
		float u, v;
		float cellSizeX = width / size;
		float cellSizeY = height / size;
		float usize = endU - startU;
		float vsize = endV - startV;
		float cellSizeU = usize / size;
		float cellSizeV = vsize / size;
		float endY = startY + height;
		float endX = startX + width;
		float color = Color.WHITE.toFloatBits();
		final int vertCount = 6 * (size+1)*(size+1);
		final int indiceCount = 6 * size * size;
		verts = new float[vertCount];
		indices = new short[indiceCount];

		Gdx.app.log("DEBUG", "STARTX=" + startX + ", STARTY=" + startY + ", W=" + width + ", H=" + height + ", INCY=" + cellSizeY + ", INCX=" + cellSizeX);

		for (y = startY, v = endV; y <= endY; y += cellSizeY, v -= cellSizeV) {
			for (x = startX, u = startU; x <= endX; x += cellSizeX, u += cellSizeU) {
				Gdx.app.log("DEBUG", "VERTEX[" + i/6 + "]=" + x + ", " + y);
				// Bottom left vertex
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = color;
				verts[i++] = u; // U
				verts[i++] = v; // V
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
				tL = (short) (bL + size + 1);
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
				
				Gdx.app.log("DEBUG", "TRIANGLE1:" + bL + ", " + tL + ", " + tR);
				Gdx.app.log("DEBUG", "TRIANGLE2:" + bL + ", " + tR + ", " + bR);

			}
		}
	}
}
