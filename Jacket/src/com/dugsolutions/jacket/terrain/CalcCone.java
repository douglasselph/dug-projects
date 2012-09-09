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
	protected float		mAB;
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
				if (dX != 0)
				{
					float angleT = (float) Math.atan(dY / dX);

					if (dX < 0)
					{
						angleT += Math.PI;
					}
					maxDist = getDistOnEllipse(angleT);
				}
				else
				{
					maxDist = mB;
				}
			}
			if (dist <= maxDist)
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

	/**
	 * Return the distance from the center the point on the ellipse would be at the given angle.
	 * 
	 * @param angleT
	 * @return
	 */
	protected float getDistOnEllipse(float angleT)
	{
		float termB = mB * FloatMath.cos(angleT);
		float termA = mA * FloatMath.sin(angleT);
		return mAB / FloatMath.sqrt(termB * termB + termA * termA);
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
			mAB = mA * mB;
		}
	}
}
