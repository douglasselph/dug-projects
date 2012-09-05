package com.dugsolutions.jacket.terrain;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * A center point where the height is at max.
 * Then a slope defines a parabolic slope outward toward the edge, max circle radius,
 * where the value is zero.
 */
public class CalcConeParabola extends CalcConstant
{

	float	mCenterX;
	float	mCenterY;
	float	mA;

	/**
	 * @param a
	 *        : defines shape of parabola.
	 * @param height
	 *        : max height at the center point.
	 */
	public CalcConeParabola(float height, float a)
	{
		super(height);
		mA = a;
	}

	public CalcConeParabola(float height, float a, Bounds2D bounds)
	{
		super(height, bounds);
		mA = a;
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float deltaX = x - mCenterX;
			float deltaY = y - mCenterY;
			float deltaXSquared = deltaX * deltaX;
			float deltaYSquared = deltaY * deltaY;
			float dist = FloatMath.sqrt(deltaXSquared + deltaYSquared);
			/*
			 * Terms:
			 * py = mA * px^2 is the equation of the parabola
			 * px is the distance of the given point to the center point.
			 * py is used to determine the computed height. Where the further out
			 * the less height is seen. And where at the center is the max
			 * value of height.
			 */
			float py = Math.abs(mA * dist * dist);
			float val = mHeight - py;
			if (val > 0)
			{
				info.addHeight(val);

				if (info.genNormal())
				{
					Vector3f normal;
					normal = new Vector3f(mCenterX - x, mCenterY - y, val);
					normal.normalize();
					info.addNormal(normal);
				}
			}
		}
	}

	public void setCenter(float x, float y)
	{
		mCenterX = x;
		mCenterY = y;
		float maxDist = FloatMath.sqrt(mHeight / mA);
		setBounds(new Bounds2D(x - maxDist, y - maxDist, x + maxDist, y + maxDist));
	}
}
