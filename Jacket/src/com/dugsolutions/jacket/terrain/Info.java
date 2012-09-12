package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Vector3f;

/**
 * This is what is computed at a given point for a generator.
 */
public class Info
{
	boolean		mGenNormal;
	float		mHeight;
	Vector3f	mNormal;
	float		mXAdjust;
	float		mYAdjust;

	public Info()
	{
	}

	public Info(final Info cp)
	{
		set(cp);
	}

	public Info addHeight(float height)
	{
		mHeight += height;
		return this;
	}

	public Info addNormal(Vector3f normal)
	{
		if (mNormal == null)
		{
			mNormal = normal;
		}
		else
		{
			mNormal.add(normal);
			mNormal.normalize();
		}
		return this;
	}

	public Info addXAdjust(float xadjust)
	{
		mXAdjust += xadjust;
		return this;
	}

	public Info addYAdjust(float yadjust)
	{
		mYAdjust += yadjust;
		return this;
	}

	public Info dup()
	{
		Info cp = new Info();

		cp.mGenNormal = mGenNormal;
		cp.mHeight = mHeight;
		if (mNormal != null)
		{
			cp.mNormal = new Vector3f(mNormal);
		}
		cp.mXAdjust = mXAdjust;
		cp.mYAdjust = mYAdjust;
		return cp;
	}

	public boolean genNormal()
	{
		return mGenNormal;
	}

	public float getHeight()
	{
		return mHeight;
	}

	public float getKey()
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

	public void set(final Info info)
	{
		mGenNormal = info.mGenNormal;
		mHeight = info.mHeight;
		mNormal = info.mNormal;
		mXAdjust = info.mXAdjust;
		mYAdjust = info.mYAdjust;
	}

	public Info setGenNormal(boolean flag)
	{
		mGenNormal = flag;
		return this;
	}

	public Info setHeight(float height)
	{
		mHeight = height;
		return this;
	}

	public Info setNormal(Vector3f normal)
	{
		mNormal = normal;
		return this;
	}

	public Info setXAdjust(float xadjust)
	{
		mXAdjust = xadjust;
		return this;
	}

	public Info setYXAdjust(float yadjust)
	{
		mYAdjust = yadjust;
		return this;
	}
}
