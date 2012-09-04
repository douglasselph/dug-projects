package com.tipsolutions.jacket.math;

import android.util.FloatMath;

public class MathUtils
{

	/** A "close to zero" double epsilon value for use */
	public static final double	EPSILON		= 2.220446049250313E-16d;

	static final int[]			POW_OF_2	= {
			1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288,
			1048576						};

	public static final float	TO_DEGREES	= (float) (180.0 / Math.PI);
	public static final float	TO_RADIANS	= (float) (Math.PI / 180.0);

	public static double clamp(double v)
	{
		while (v < 0)
		{
			v += Constants.TWO_PI;
		}
		while (v >= Constants.TWO_PI)
		{
			v -= Constants.TWO_PI;
		}
		return v;
	}

	public static float clamp(float v)
	{
		while (v < 0)
		{
			v += Constants.TWO_PI;
		}
		while (v >= Constants.TWO_PI)
		{
			v -= Constants.TWO_PI;
		}
		return v;
	}

	/**
	 * Return the distance bewteen the two specified points.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static float distance(float x1, float y1, float x2, float y2)
	{
		float deltaX = x1 - x2;
		float deltaY = y1 - y2;
		float deltaXSquared = deltaX * deltaX;
		float deltaYSquared = deltaY * deltaY;
		return FloatMath.sqrt(deltaXSquared + deltaYSquared);
	}

	public static double inverseSqrt(final double dValue)
	{
		return 1 / Math.sqrt(dValue);
	}

	public static int powOf2(int arg)
	{
		if (arg >= 0 && arg < POW_OF_2.length)
		{
			return POW_OF_2[arg];
		}
		return 0;
	}

	public static float toDegrees(float radians)
	{
		return radians * TO_DEGREES;
	}

	public static float toRadians(float degrees)
	{
		return degrees * TO_RADIANS;
	}

	public static float LARGER(float v1, float v2)
	{
		return v1 >= v2 ? v1 : v2;
	}

	public static float LESSER(float v1, float v2)
	{
		return v1 <= v2 ? v1 : v2;
	}
}
