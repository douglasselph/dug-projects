package com.tipsolutions.jacket.math;

public class MaterialColors
{
	Color4f	mAmbient;
	Color4f	mDiffuse;
	Color4f	mSpecular;
	Color4f	mEmission;
	Float	mShininess;

	public MaterialColors()
	{
	}

	public MaterialColors(final MaterialColors cp)
	{
		copy(cp);
	}

	public void copy(final MaterialColors cp)
	{
		if (cp.mAmbient == null)
		{
			mAmbient = null;
		}
		else
		{
			mAmbient = new Color4f(cp.mAmbient);
		}
		if (cp.mDiffuse == null)
		{
			mDiffuse = null;
		}
		else
		{
			mDiffuse = new Color4f(cp.mDiffuse);
		}
		if (cp.mSpecular == null)
		{
			mSpecular = null;
		}
		else
		{
			mSpecular = new Color4f(cp.mSpecular);
		}
		if (cp.mEmission == null)
		{
			mEmission = null;
		}
		else
		{
			mEmission = new Color4f(cp.mEmission);
		}
		if (cp.mShininess == null)
		{
			mShininess = null;
		}
		else
		{
			mShininess = new Float(cp.mShininess);
		}
	}

	public Color4f getAmbient()
	{
		return mAmbient;
	}

	public Color4f getDiffuse()
	{
		return mDiffuse;
	}

	public Color4f getEmission()
	{
		return mEmission;
	}

	public Float getShininess()
	{
		return mShininess;
	}

	public Color4f getSpecular()
	{
		return mSpecular;
	}

	public MaterialColors setAmbient(Color4f color)
	{
		mAmbient = color;
		return this;
	}

	public MaterialColors setDiffuse(Color4f color)
	{
		mDiffuse = color;
		return this;
	}

	public MaterialColors setEmission(Color4f color)
	{
		mEmission = color;
		return this;
	}

	public MaterialColors setShininess(float shininess)
	{
		mShininess = shininess;
		return this;
	}

	public MaterialColors setSpecular(Color4f color)
	{
		mSpecular = color;
		return this;
	}
}
