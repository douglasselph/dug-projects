package com.dugsolutions.jacket.terrain;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * A center point where the height is at max.
 * 
 * Then a slope defines a parabolic slope outward toward the edge, max circle radius,
 * where the value is zero.
 */
public class CalcMound extends CalcConstant
{
	protected float		mA;		// Ellipsoid X axis length.
	protected float		mB;		// Ellipsoid y axis length.
	protected float		mCenterX;	// Center of circle and ellipse
	protected float		mCenterY;
	protected float		mMaxDist;	// Distance greater than or equal to this is always zero.
	protected boolean	mIsCircle;	// Otherwise ellipse which is more complicated
	protected float		mPA;

	public CalcMound(float height, Bounds2D bounds)
	{
		super(height, bounds);
		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float dX = x - mCenterX;
			float dY = y - mCenterY;
			float dXSquared = dX * dX;
			float dYSquared = dY * dY;
			float height;
			float dist = FloatMath.sqrt(dXSquared + dYSquared);

			if (mIsCircle)
			{
				height = -mPA * dist * dist + mHeight;
			}
			else
			{
				float maxDist;

				if (dX != 0)
				{
					float angleT = (float) Math.atan(dY / dX);

					if (x < 0)
					{
						angleT += Math.PI;
					}
					/*
					 * Find point along line defined by angleT that is on the ellipse, which is
					 * also the max distance.
					 */
					float maxX = mA * FloatMath.cos(angleT);
					float maxY = mB * FloatMath.sin(angleT);

					maxDist = FloatMath.sqrt(maxX * maxX + maxY * maxY);
				}
				else
				{
					maxDist = mB;
				}
				height = -(mHeight / maxDist) * dist * dist + mHeight;
			}
			if (height > 0)
			{
				info.addHeight(height);

				if (info.genNormal())
				{
					Vector3f normal;
					normal = new Vector3f(dX, dY, -height);
					normal.normalize();
					info.addNormal(normal);
				}
			}
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}

	protected void init()
	{
		if (mHeight > 0)
		{
			mCenterX = mBounds.getMidX();
			mCenterY = mBounds.getMidY();
			mIsCircle = mBounds.isSquare();
			mA = mBounds.getSizeX() / 2;
			mB = mBounds.getSizeY() / 2;
			mPA = mHeight / mA;
		}
	}
}
