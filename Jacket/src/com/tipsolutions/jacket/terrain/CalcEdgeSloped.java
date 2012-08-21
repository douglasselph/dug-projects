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

	float getAdjust(float cminval, float cmaxval, float val)
	{
		float valDist = (val - cminval) / (cmaxval - cminval);
		float adjusted = mMinValueAdjust + mSizeValueAdjust * valDist;
		return adjusted;
	}

	float getAdjustX(float x, float y)
	{
		float adjX = getAdjust(mBounds.getMinY(), mBounds.getMaxY(), y);
		float newX = x + adjX;
		if (newX < mBounds.getMinX())
		{
			newX = mBounds.getMinX();
		}
		else if (newX > mBounds.getMaxX())
		{
			newX = mBounds.getMaxX();
		}
		return newX - x;
	}

	float getAdjustY(float x, float y)
	{
		float adjY = getAdjust(mBounds.getMinX(), mBounds.getMaxX(), x);
		float newY = y + adjY;
		if (newY < mBounds.getMinY())
		{
			newY = mBounds.getMinY();
		}
		else if (newY > mBounds.getMaxY())
		{
			newY = mBounds.getMaxY();
		}
		return newY - y;
	}

	@Override
	public Info getInfo(float x, float y)
	{
		if (within(x, y))
		{
			if (mOrientation == Orientation.HORIZONTAL)
			{
				return new Info(0f, getAdjustY(x, y));
			}
			else
			{
				return new Info(getAdjustX(x, y), 0f);
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
