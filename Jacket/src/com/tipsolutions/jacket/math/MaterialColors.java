package com.tipsolutions.jacket.math;

public class MaterialColors
{
	Color4f	mAmbient;
	Color4f	mDiffuse;
	Color4f	mEmission;
	Float	mShininess;
	Color4f	mSpecular;

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
