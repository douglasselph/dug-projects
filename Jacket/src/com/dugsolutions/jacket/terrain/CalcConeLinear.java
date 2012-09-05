package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * A simple cone generator. There is a defined center point where the max height is.
 * And then there is a radius where the outer edge is zero. A circle defines the entire linear
 * slope that is seen.
 */
public class CalcConeLinear extends CalcConstant
{
	float	mA;		// Semi major axis or distance from center of ellipse to right edge
	float	mB;		// Semi minor axis or distance from center of ellipse to top edge.
	float	mAB;		// mA * mB
	float	mCenterX;
	float	mCenterY;
	boolean	mIsCircle;	// otherwise ellipse which is more complicated
	float	mMaxDist;	// used for circle only.

	public CalcConeLinear(float height)
	{
		super(height);
	}

	public CalcConeLinear(float height, Bounds2D bounds)
	{
		super(height, bounds);
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float percentX = 1f - percent(x, mCenterX, mBounds.getSizeX() / 2);
			float percentY = 1f - percent(y, mCenterY, mBounds.getSizeY() / 2);

			if (percentX > 0 && percentX <= 1 && percentY > 0 && percentY <= 1)
			{
				float percent = percentX * percentY;
				float height = mHeight * percent;

				info.addHeight(height);

				if (info.genNormal())
				{
					Vector3f normal;

					normal = new Vector3f(x - mCenterX, y - mCenterY, height);
					normal.normalize();
					info.addNormal(normal);
				}
			}
		}
	}

	/**
	 * Return the percentage of the distance from the center point that the designated point is.
	 * If x is right on top of cx then that is 0% away. If it is sizex or greater then it is 100% away.
	 * 
	 * @param x
	 * @param cx
	 * @param sizex
	 * @return
	 */
	float percent(float x, float cx, float sizex)
	{
		return Math.abs(x - cx) / sizex;
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		mCenterX = mBounds.getMidX();
		mCenterY = mBounds.getMidY();
		mMaxDist = Math.abs(mBounds.getMidX() - mCenterX);
		mIsCircle = mBounds.isSquare();
		mA = mBounds.getSizeX() / 2;
		mB = mBounds.getSizeY() / 2;
		mAB = mA * mB;
	}

}
