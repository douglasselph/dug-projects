package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;
import java.util.Random;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;

public class CalcEdgeJagged extends CalcValue
{
	class Jag
	{
		final float[]	mAdjust;
		final float		mDelta;
		final float		mVariance;

		Jag(int count, float variance)
		{
			mVariance = variance;
			mAdjust = new float[count];

			float twiceVar = mVariance * 2;

			for (int i = 0; i < mAdjust.length; i++)
			{
				mAdjust[i] = twiceVar * mRand.nextFloat() - mVariance;
			}
			mDelta = 1f / (mAdjust.length - 1);
		}

		float getAdjust(float valDist)
		{
			float index = (mAdjust.length - 1) * valDist;
			int index1 = (int) FloatMath.floor(index);
			int index2 = (int) FloatMath.ceil(index);
			float val1 = mAdjust[index1];
			float val2 = mAdjust[index2];
			float valDelta = val2 - val1;
			float distIndex1 = index1 * mDelta;
			float indexDelta = (valDist - distIndex1) / mDelta;
			return val1 + valDelta * indexDelta;
		}

		int getNumPts()
		{
			return mAdjust.length;
		}
	}

	public enum Orientation
	{
		VERTICAL, HORIZONTAL
	}

	Orientation		mOrientation;
	ArrayList<Jag>	mJags	= new ArrayList<Jag>();
	Random			mRand;

	public CalcEdgeJagged(Bounds2D bounds)
	{
		this(bounds, 0);
	}

	public CalcEdgeJagged(Bounds2D bounds, long seed)
	{
		super(bounds);
		mOrientation = (bounds.getSizeX() > bounds.getSizeY() ? Orientation.HORIZONTAL : Orientation.VERTICAL);
		mRand = new Random(seed);
	}

	public CalcEdgeJagged addJag(int count, float variance)
	{
		mJags.add(new Jag(count, variance));
		return this;
	}

	float getAdjust(float minval, float maxval, float val)
	{
		float amt = 0;
		float valDist = (val - minval) / (maxval - minval);

		for (Jag jag : mJags)
		{
			amt += jag.getAdjust(valDist);
		}
		return amt;
	}

	public int getMaxJagPts()
	{
		int count = 0;
		for (Jag jag : mJags)
		{
			if (jag.getNumPts() > count)
			{
				count = jag.getNumPts();
			}
		}
		return count;
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			if (mOrientation == Orientation.HORIZONTAL)
			{
				info.addYAdjust(getAdjust(mBounds.getMinX(), mBounds.getMaxX(), x));
			}
			else
			{
				info.addXAdjust(getAdjust(mBounds.getMinY(), mBounds.getMaxY(), y));
			}
		}
	}

	public CalcEdgeJagged setOrientation(Orientation orientation)
	{
		mOrientation = orientation;
		return this;
	}
}
