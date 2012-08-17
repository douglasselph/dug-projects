package com.tipsolutions.jacket.terrain;

import com.tipsolutions.jacket.math.Bounds2D;

public class CalcEdgeSloped extends CalcValue
{
	public enum Orientation
	{
		VERTICAL, HORIZONTAL
	}

	float		mMinValueAdjust;
	float		mMaxValueAdjust;
	float		mSizeValueAdjust;
	Orientation	mOrientation;

	public CalcEdgeSloped()
	{
		super();
	}

	public CalcEdgeSloped(Bounds2D bounds, float minValue, float maxValue)
	{
		super(bounds);
		mMinValueAdjust = minValue;
		mMaxValueAdjust = maxValue;
		mSizeValueAdjust = mMaxValueAdjust - mMinValueAdjust;
		mOrientation = (bounds.getSizeX() > bounds.getSizeY() ? Orientation.HORIZONTAL : Orientation.VERTICAL);
	}

	float getAdjust(float minval, float maxval, float val)
	{
		float valDist = (val - minval) / (maxval - minval);
		return mMinValueAdjust + mSizeValueAdjust * valDist;
	}

	@Override
	public Info getInfo(float x, float y)
	{
		if (within(x, y))
		{
			if (mOrientation == Orientation.HORIZONTAL)
			{
				return new Info(0f, getAdjust(mBounds.getMinX(), mBounds.getMaxX(), x));
			}
			else
			{
				return new Info(getAdjust(mBounds.getMinY(), mBounds.getMaxY(), y), 0f);
			}
		}
		return null;
	}

	public CalcEdgeSloped setOrientation(Orientation orientation)
	{
		mOrientation = orientation;
		return this;
	}
}
