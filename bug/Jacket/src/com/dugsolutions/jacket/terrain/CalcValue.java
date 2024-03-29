package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;

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
		setBounds(bounds);
	}

	public void fillInfo(float x, float y, Info info)
	{
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

	@Override
	public void postCalc(IMapData query)
	{
	}
}
