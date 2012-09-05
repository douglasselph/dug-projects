package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;

/**
 * Generate a constant height over all values.
 * Can be sub-classed for generators that have at least a bounds and a height.
 */
public class CalcConstant extends CalcValue
{
	protected float	mHeight;

	public CalcConstant(float height)
	{
		mHeight = height;
	}

	public CalcConstant(float height, Bounds2D bounds)
	{
		super(bounds);
		mHeight = height;
	}

	public float getHeight()
	{
		return mHeight;
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			info.addHeight(mHeight);
		}
	}

}
