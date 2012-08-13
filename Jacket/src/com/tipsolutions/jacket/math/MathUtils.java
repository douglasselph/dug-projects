package com.tipsolutions.jacket.math;

public class MathUtils
{

	/** A "close to zero" double epsilon value for use */
	public static final double	EPSILON	= 2.220446049250313E-16d;

	public static double inverseSqrt(final double dValue)
	{
		return 1 / Math.sqrt(dValue);
	}

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

	static final int[]	POW_OF_2	= { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512,
			1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144,
			524288, 1048576		};

	public static int powOf2(int arg)
	{
		if (arg >= 0 && arg < POW_OF_2.length)
		{
			return POW_OF_2[arg];
		}
		return 0;
	}

}
