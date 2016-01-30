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
public class CalcMound extends CalcCone
{
	protected float	mPA;

	public CalcMound(float height, Bounds2D bounds)
	{
		super(height, bounds);
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
				// Parabola along the line in the ellipsoid of interest.
				height = -(mHeight / (maxDist * maxDist)) * dist * dist + mHeight;
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
	protected void init()
	{
		super.init();

		if (mHeight > 0)
		{
			mPA = mHeight / (mA * mA);
		}
	}
}
