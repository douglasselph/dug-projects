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
	protected float	mdS;
	protected float	mPA;

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
			if (mIsCircle)
			{
				float dX = x - mCenterX;
				float dY = y - mCenterY;
				float dist = FloatMath.sqrt(dX * dX + dY * dY);
				float height = -mdS * dist * dist + mHeight;

				if (height > 0)
				{
					info.addHeight(height);

					if (info.genNormal())
					{
						Vector3f normal;
						normal = new Vector3f(mCenterX - x, mCenterY - y, height);
						normal.normalize();
						info.addNormal(normal);
					}
				}
			}
			else
			{
				float f1;
				float f2;
				float dA;
				float dB;
				float dP;
				float dist;
				float height;

				if (mdX > mdY)
				{
					f1 = mCenterX - mC;
					f2 = mCenterX + mC;
					dB = y - mCenterY;
					dP = x;
				}
				else
				{
					f1 = mCenterY - mC;
					f2 = mCenterY + mC;
					dB = x - mCenterX;
					dP = y;
				}
				if (dP < f1)
				{
					dA = dP - f1;
					dist = FloatMath.sqrt(dA * dA + dB * dB);
				}
				else if (dP > f2)
				{
					dA = dP - f2;
					dist = FloatMath.sqrt(dA * dA + dB * dB);
				}
				else
				{
					dist = dB;
					dA = 0;
				}
				height = -mdS * dist * dist + mHeight;

				if (height > 0)
				{
					info.addHeight(height);

					if (info.genNormal())
					{
						float dX;
						float dY;

						if (mdX > mdY)
						{
							dY = dB;

							if (x >= f2)
							{
								dX = dA;
							}
							else if (x <= f1)
							{
								dX = dA;
							}
							else
							{
								dX = 0;
							}
						}
						else
						{
							dX = dB;

							if (y >= f2)
							{
								dY = dA;
							}
							else if (y <= f1)
							{
								dY = dA;
							}
							else
							{
								dY = 0;
							}
						}
						Vector3f normal;
						normal = new Vector3f(dX, dY, height);
						normal.normalize();
						info.addNormal(normal);
					}
				}
			}
		}
	}

	void init()
	{
		if (mHeight != 0)
		{
			if (mdX > mdY)
			{
				mdS = mdY;
			}
			else
			{
				mdS = mdX;
			}
			mPA = mHeight / mdS;
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}

}
