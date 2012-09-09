package com.dugsolutions.bugplug.map;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcMound;

public class CalcMountainRidge extends CalcGroup
{
	Bounds2D	mBounds;
	final float	mHeight;
	final float	mRidgeSize;
	final float	mRidgeRatio;

	/**
	 * 
	 * @param bounds
	 * @param height
	 * @param ridgeSize
	 * @param leftRatio
	 *        : the percentage of how much longer the left side is from the right side. If 1, they are equal. If < 1,
	 *        then the left side is shorter than the right. If > 1, then it is longer by that much.
	 */
	public CalcMountainRidge(float height, float ridgeSize, float leftRatio, Bounds2D bounds)
	{
		super();
		mBounds = bounds;
		mHeight = height;
		mRidgeSize = ridgeSize;
		mRidgeRatio = leftRatio;
		init();
	}

	void init()
	{
		float leftRidgeLength;
		float rightRidgeLength;

		if (mRidgeRatio < 1)
		{
			rightRidgeLength = mBounds.getSizeY();
			leftRidgeLength = mBounds.getSizeY() * mRidgeRatio;
		}
		else
		{
			leftRidgeLength = mBounds.getSizeY();
			rightRidgeLength = mBounds.getSizeY() / mRidgeRatio;
		}
		float xmin;
		float ymin;
		float xmax;
		float ymax;
		Bounds2D edge;
		// Left rise
		xmin = mBounds.getMinX();
		xmax = xmin + mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - leftRidgeLength;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mHeight, edge));

		// Top rise
		xmin = mBounds.getMinX();
		xmax = mBounds.getMaxX();
		ymax = mBounds.getMaxY();
		ymin = ymax - mRidgeSize;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mHeight, edge));

		// Right rise
		xmax = mBounds.getMaxX();
		xmin = xmax - mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - rightRidgeLength;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mHeight, edge));
	}
}
