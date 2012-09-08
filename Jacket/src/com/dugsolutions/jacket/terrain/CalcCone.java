package com.dugsolutions.jacket.terrain;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * A Cone Generator.
 * 
 * There is a defined boundary which indicates either a circular area or an elliptical area.
 * If it is a circle, the center of the circle is the height. The edge of the circle is zero.
 * If it is an ellipse, the two foci of the ellipse and the line connecting them, is the height.
 * The distance from the foci determines what percentage of the height to use until the edge of the ellipse is reached
 * which is zero.
 */
public class CalcCone extends CalcConstant
{
	protected float		mA;
	protected float		mB;
	protected float		mCenterX;	// Center of circle and ellipse
	protected float		mCenterY;
	protected boolean	mIsCircle;	// Otherwise ellipse which is more complicated

	public CalcCone(float height, Bounds2D bounds)
	{
		super(height, bounds);
		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float height;
			float percent;
			float dX = x - mCenterX;
			float dY = y - mCenterY;
			float dXSquared = dX * dX;
			float dYSquared = dY * dY;
			float dist = FloatMath.sqrt(dXSquared + dYSquared);
			float maxDist;

			if (mIsCircle)
			{
				maxDist = mA;
			}
			else
			{
				float angleT;

				if (dX != 0)
				{
					angleT = (float) Math.atan(dY / dX);

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
			}
			if (dist < maxDist)
			{
				percent = 1 - dist / maxDist;
				height = percent * mHeight;
				info.addHeight(height);

				if (info.genNormal() && height > 0)
				{
					Vector3f normal;
					normal = new Vector3f(dX, dY, height);
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
		}
	}
}
