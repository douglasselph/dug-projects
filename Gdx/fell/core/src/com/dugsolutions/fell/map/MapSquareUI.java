package com.dugsolutions.fell.map;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class MapSquareUI extends MapSquareData {
	// This square is displaying this region in the atlas.
	AtlasRegion region;
	Mesh mesh;

	public MapSquareUI(AtlasRegion r) {
		region = r;
	}

	public void build(float startX, float startY, float width, float height,
			float startZ, float scaleZ) {
		build(startX, startY, width, height, region.getU(), region.getV(),
				region.getU2(), region.getV2(), startZ, scaleZ);

		mesh = new Mesh(true, verts.length / 6, indices.length,
				new VertexAttribute(VertexAttributes.Usage.Position, 3,
						ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
						VertexAttributes.Usage.ColorPacked, 4,
						ShaderProgram.COLOR_ATTRIBUTE), new VertexAttribute(
						VertexAttributes.Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);
		mesh.setIndices(indices);
	}

	public void render(ShaderProgram program, Matrix4 projMatrix) {
		region.getTexture().bind();
		program.begin();
		program.setUniformMatrix("u_projTrans", projMatrix);
		program.setUniformi("u_texture", 0);
		mesh.render(program, GL20.GL_TRIANGLES);
		program.end();
	}
}
