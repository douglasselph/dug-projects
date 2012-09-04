package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Vector3f;

/**
 * This is what is computed at a given point for a generator.
 */
public class Info
{
	float		mHeight;
	Vector3f	mNormal;
	float		mXAdjust;
	float		mYAdjust;

	public Info()
	{
	}

	public Info(float height)
	{
		mHeight = height;
		mNormal = new Vector3f(0, 0, 1);
	}

	public Info(float xadjust, float yadjust)
	{
		mXAdjust = xadjust;
		mYAdjust = yadjust;
	}

	public Info(float height, Vector3f normal)
	{
		mHeight = height;
		mNormal = normal;
	}

	public Info(float height, Vector3f normal, float xadjust, float yadjust)
	{
		mHeight = height;
		mNormal = normal;
		mXAdjust = xadjust;
		mYAdjust = yadjust;
	}

	public float getKey()
	{
		return mHeight;
	}

	public float getHeight()
	{
		return mHeight;
	}

	public Vector3f getNormal()
	{
		return mNormal;
	}

	public float getXAdjust()
	{
		return mXAdjust;
	}

	public float getYAdjust()
	{
		return mYAdjust;
	}

	public void setHeight(float height)
	{
		mHeight = height;
	}

	public void setNormal(Vector3f normal)
	{
		mNormal = normal;
	}

	public void setXAdjust(float xadjust)
	{
		mXAdjust = xadjust;
	}

	public void setYXAdjust(float yadjust)
	{
		mYAdjust = yadjust;
	}

	public Info add(final Info arg)
	{
		mHeight += arg.mHeight;
		mXAdjust += arg.mXAdjust;
		mYAdjust += arg.mYAdjust;

		if (arg.getNormal() != null)
		{
			if (mNormal == null)
			{
				mNormal = arg.getNormal();
			}
			else
			{
				mNormal.add(arg.getNormal());
				mNormal.normalize();
			}
		}
		return this;
	}
}
