package com.dugsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.util.FloatMath;

import com.dugsolutions.jacket.image.Texture;
import com.dugsolutions.jacket.math.Bounds3D;

public class Pyramid extends Model
{

	public Pyramid()
	{
	}

	public Pyramid(float base, float height)
	{
		this(base, height, null);
	}

	// Make a four sided pyramid the given base side length
	// and height. The y-axis will serve as the center pole
	// defining the height. The forward face will be parallel
	// to the x-axis.
	//
	// Assumes CCW facing.
	public Pyramid(float base, float height, Texture texture)
	{
		float radians60 = (float) (Math.PI / 3);
		float triHeight = FloatMath.sin(radians60) / FloatMath.cos(radians60) * base / 2;
		final float baseHalf = base / 2;
		final float triHeightHalf = triHeight / 2;
		{
			FloatBuffer buf = initVertexBuf(4 * 3);
			buf.put(0).put(height).put(0f); /* 0: peak */
			buf.put(-baseHalf).put(0f).put(triHeightHalf); /* 1: x-left */
			buf.put(baseHalf).put(0f).put(triHeightHalf); /* 2: x-right */
			buf.put(0f).put(0f).put(-triHeightHalf); /* 3: z-depth */
		}
		{
			ShortBuffer buf = initIndexBuf(4 * 3);
			buf.put((short) 0).put((short) 1).put((short) 2); /* face front */
			buf.put((short) 0).put((short) 3).put((short) 1); /* face left */
			buf.put((short) 0).put((short) 2).put((short) 3); /* face right */
			buf.put((short) 1).put((short) 3).put((short) 2); /* base */
		}
		if (texture != null)
		{
			setTexture(texture);
			{
				FloatBuffer buf = initTextureBuf(3 * 2);
				buf.put(0.5f).put(1);/* 0: peak */
				buf.put(0f).put(0f); /* 1: x-left */
				buf.put(1f).put(0f); /* 2: x-right */
			}
		}
		mBounds = new Bounds3D();
		mBounds.setMinX(-baseHalf);
		mBounds.setMaxX(baseHalf);
		mBounds.setMinY(0);
		mBounds.setMaxY(height);
		mBounds.setMinZ(-triHeightHalf);
		mBounds.setMaxZ(triHeightHalf);
	}
}
