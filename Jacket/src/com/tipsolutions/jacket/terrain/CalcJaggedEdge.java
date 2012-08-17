package com.tipsolutions.jacket.terrain;

import java.util.ArrayList;
import java.util.Random;

import android.util.FloatMath;

import com.tipsolutions.jacket.math.Bounds2D;

public class CalcJaggedEdge extends CalcValue
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

		float getAdjust(float minval, float maxval, float val)
		{
			float valDist = (val - minval) / (maxval - minval);
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
	}

	public enum Orientation
	{
		VERTICAL, HORIZONTAL
	}

	Orientation		mOrientation;
	ArrayList<Jag>	mJags	= new ArrayList<Jag>();
	Random			mRand;

	public CalcJaggedEdge(Orientation orientation, Bounds2D bounds)
	{
		this(orientation, bounds, 0);
	}

	public CalcJaggedEdge(Orientation orientation, Bounds2D bounds, long seed)
	{
		super(bounds);
		mOrientation = orientation;
		mRand = new Random(seed);
	}

	public CalcJaggedEdge addJag(int count, float variance)
	{
		mJags.add(new Jag(count, variance));
		return this;
	}

	float getAdjust(float minval, float maxval, float val)
	{
		float amt = 0;

		for (Jag jag : mJags)
		{
			amt += jag.getAdjust(minval, maxval, val);
		}
		return amt;
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

	public CalcJaggedEdge setOrientation(Orientation orientation)
	{
		mOrientation = orientation;
		return this;
	}
}
