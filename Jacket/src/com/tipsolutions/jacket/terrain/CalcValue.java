package com.tipsolutions.jacket.terrain;

import com.tipsolutions.jacket.math.Bounds2D;

/**
 * Base calc value which holds a common bounds which all generators use.
 */
public class CalcValue implements ICalcValue
{
	protected Bounds2D	mBounds;

	public CalcValue()
	{
	}

	public CalcValue(Bounds2D bounds)
	{
		mBounds = bounds;
	}

	public Info getInfo(float x, float y)
	{
		return null;
	}

	public void setBounds(Bounds2D bounds)
	{
		mBounds = bounds;
	}

	public boolean within(float x, float y)
	{
		if (mBounds == null)
		{
			return true;
		}
		return mBounds.within(x, y);
	}

}
