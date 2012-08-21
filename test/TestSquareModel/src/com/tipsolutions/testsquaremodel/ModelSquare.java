package com.tipsolutions.testsquaremodel;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.model.Model;

public class ModelSquare extends Model
{
	public ModelSquare(float length)
	{
		initVertexBuf(length);
		initIndexBuf();
		initNormalBuf();
	}

	void initIndexBuf()
	{
		/** CW: triangles */
		ShortBuffer buf = initIndexBuf(6);
		buf.put((short) 0).put((short) 1).put((short) 2);
		buf.put((short) 3).put((short) 2).put((short) 1);
		buf.rewind();
	}

	void initNormalBuf()
	{
		FloatBuffer buf = initNormalBuf(4 * 3);
		for (int i = 0; i < 4; i++)
		{
			buf.put(0).put(0).put(1);
		}
		buf.rewind();
	}

	void initTextureBuf()
	{
		final float texPts[] = {
				0f, 0f, // upper left
				1f, 0f, // upper-right
				0f, 1f, // lower-left
				1f, 1f // lower right
		};
		FloatBuffer buf = initTextureBuf(texPts.length);
		buf.put(texPts);
		buf.rewind();
	}

	void initVertexBuf(final float length)
	{
		final float vertices[] = {
				-length, length, 0f, // 0: upper-left
				length, length, 0f, // 1: upper-right
				-length, -length, 0f, // 2: lower-left
				length, -length, 0f }; // 3: lower-right

		FloatBuffer buf = initVertexBuf(vertices.length);
		buf.put(vertices);
		buf.rewind();
	}

	@Override
	protected void onDrawPre(GL10 gl)
	{
		super.onDrawPre(gl);
		gl.glFrontFace(GL10.GL_CW);
	}

	@Override
	public Model setTexture(Texture texture)
	{
		initNormalBuf();
		initTextureBuf();
		super.setTexture(texture);
		return this;
	}
}
