package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;

public class CalcHeightColor implements ICalcColor
{
	Bounds2D		mBounds;
	final Color4f	mMinColor;
	final Color4f	mMaxColor;
	final float		mMinHeight;
	final float		mMaxHeight;
	final float		mRangeH;
	final float		mRangeR;
	final float		mRangeG;
	final float		mRangeB;

	public CalcHeightColor(float minHeight, float maxHeight, Color4f minColor, Color4f maxColor, Bounds2D bounds)
	{
		mBounds = bounds;
		mMinColor = minColor;
		mMaxColor = maxColor;
		mMinHeight = minHeight;
		mMaxHeight = maxHeight;
		mRangeH = mMaxHeight - mMinHeight;
		mRangeR = maxColor.getRed() - minColor.getRed();
		mRangeG = maxColor.getGreen() - minColor.getGreen();
		mRangeB = maxColor.getBlue() - minColor.getBlue();
	}

	@Override
	public Color4f getColor(float x, float y, float height)
	{
		if (height <= mMinHeight)
		{
			return mMinColor;
		}
		if (height >= mMaxHeight)
		{
			return mMaxColor;
		}
		float percent = (height - mMinHeight) / mRangeH;
		Color4f color = new Color4f(mMinColor.getRed() + percent * mRangeR, mMinColor.getGreen() + percent * mRangeG,
				mMinColor.getBlue() + percent + mRangeB);
		return color;
	}
}
