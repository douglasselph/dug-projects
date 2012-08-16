package com.tipsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager.Texture;

public class Square extends Model
{
	public Square(float length)
	{
		initVertexBuf(length);
		initIndexBuf();
		mIndexMode = GL10.GL_TRIANGLE_STRIP;
	}

	void initIndexBuf()
	{
		/** CW: triangle strip */
		// ShortBuffer buf = initIndexBuf(4);
		ShortBuffer buf = initIndexTriStrip(4, 4);
		buf.put((short) 0).put((short) 1).put((short) 2).put((short) 3);
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
		final float texPts[] = { 0f, 1f, // top left
				0f, 0f, // bottom left
				1f, 1f, // top right
				1f, 0f // bottom right
		};
		FloatBuffer buf = initTextureBuf(texPts.length);
		buf.put(texPts);
		buf.rewind();
	}

	void initVertexBuf(final float length)
	{
		final float vertices[] = { -length, -length, 0f, // 0: lower-left
				-length, length, 0f, // 1: upper-left
				length, -length, 0f, // 2: lower-right
				length, length, 0f }; // 3: upper-right

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
