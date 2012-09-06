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
	protected float		mdX;		// Semi major axis or distance from center of ellipse to right edge
	protected float		mdY;		// Semi minor axis or distance from center of ellipse to top edge.
	protected float		mC;		// Distance from center to either foci on ellipse
	protected float		mCenterX;	// Center of circle and ellipse
	protected float		mCenterY;
	protected float		mMaxDist;	// Distance greater than or equal to this is always zero.
	protected float		mMinDist;	// Distance less than or equal to this is full height.
	protected boolean	mIsCircle;	// Otherwise ellipse which is more complicated

	public CalcCone(float height, Bounds2D bounds)
	{
		super(height, bounds);
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float height;
			float dist;
			float percent;

			if (mIsCircle)
			{
				float dX = x - mCenterX;
				float dY = y - mCenterY;
				dist = FloatMath.sqrt(dX * dX + dY * dY);

				if (dist < mMaxDist)
				{
					percent = 1 - dist / mMaxDist;
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
			else
			{
				float f1;
				float f2;
				float dA1;
				float dA2;
				float dB;

				if (mdX > mdY)
				{
					f1 = mCenterX - mC;
					f2 = mCenterX + mC;
					dA1 = x - f1;
					dA2 = x - f2;
					dB = y - mCenterY;
				}
				else
				{
					f1 = mCenterY - mC;
					f2 = mCenterY + mC;
					dA1 = y - f1;
					dA2 = y - f2;
					dB = x - mCenterX;
				}
				float dBSquared = dB * dB;
				float dist1 = FloatMath.sqrt(dA1 * dA1 + dBSquared);
				float dist2 = FloatMath.sqrt(dA2 * dA2 + dBSquared);
				dist = dist1 + dist2;

				if (dist <= mMinDist)
				{
					height = mHeight;
				}
				else if (dist < mMaxDist)
				{
					percent = 1 - ((dist - mMinDist) / (mMaxDist - mMinDist));
					height = percent * mHeight;
				}
				else
				{
					height = 0;
				}
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
								dX = dA2;
							}
							else if (x <= f1)
							{
								dX = dA1;
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
								dY = dA2;
							}
							else if (y <= f1)
							{
								dY = dA1;
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

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		mCenterX = mBounds.getMidX();
		mCenterY = mBounds.getMidY();
		mIsCircle = mBounds.isSquare();
		mdX = mBounds.getSizeX() / 2;
		mdY = mBounds.getSizeY() / 2;

		if (mIsCircle)
		{
			mMinDist = 0;
			mMaxDist = mdX;
		}
		else
		{
			mC = FloatMath.sqrt(mdX * mdX + mdY * mdY);
			mMinDist = 2 * mC;

			if (mdX > mdY)
			{
				mMaxDist = mdX * 2;
			}
			else
			{
				mMaxDist = mdY * 2;
			}
		}
	}
}
