package com.tipsolutions.jacket.terrain;

import android.util.FloatMath;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.terrain.HeightMap.DataPoint;

/**
 * Calculate a mountain range where the peak is directly in the middle of the boundary defined.
 * 
 * The diamond/square algorithm is used to determine a grid of values in which to gather any particular height.
 * The actual height will be determined by the average within the principle values that a particular point is.
 */
public class CalcMountain extends CalcConstant
{
	HeightMap	mHeightMap;
	/** How many real units per height map unit to use. */
	int			mDetail;

	/**
	 * @param height
	 *        : The height at the peak.
	 * @param roughness
	 *        : How rough the terrain is. 1.0 is 100% fantastically rough. 0.1f is 10% roughness.
	 * @param detail
	 *        : How many points to compute per unit of actual boundary.
	 * @param seed
	 *        : random value seed
	 */
	public CalcMountain(float height, float roughness, int detail, long seed)
	{
		super(height);
		init(roughness, detail, seed);
	}

	/**
	 * 
	 * @param height
	 *        : The height at the peak.
	 * @param roughness
	 *        : How rough the terrain is. 1.0 is 100% fantastically rough. 0.1f is 10% roughness.
	 * @param detail
	 *        : How many points to compute per unit of actual boundary.
	 * @param seed
	 *        : random value seed
	 * @param bounds
	 *        : boundaries to apply mountain too.
	 */
	public CalcMountain(float height, float roughness, int detail, long seed, Bounds2D bounds)
	{
		super(height, bounds);
		init(roughness, detail, seed);
	}

	public CalcMountain(float height, int detail, long seed, Bounds2D bounds)
	{
		this(height, 0.1f, detail, seed, bounds);
	}

	@Override
	public Info getInfo(float x, float y)
	{
		if (!within(x, y))
		{
			return null;
		}

		float percentX = mBounds.percentX(x);
		float percentY = mBounds.percentY(y);

		float fX = mHeightMap.getPosX(percentX);
		float fY = mHeightMap.getPosY(percentY);
		int iXLo = (int) FloatMath.floor(fX);
		int iXHi = (int) FloatMath.ceil(fX);
		int iYLo = (int) FloatMath.floor(fY);
		int iYHi = (int) FloatMath.ceil(fY);
		float iXLoP = fX - iXLo;
		float iYLoP = fY - iYLo;
		float iXHiP = 1 - iXLoP;
		float iYHiP = 1 - iYLoP;

		if (iXLo == iXHi)
		{
			if (iYLo == iYHi)
			{
				DataPoint dataPt = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYLo);
				return new Info(dataPt.getHeight(), dataPt.getNormal());
			}
			else
			{
				DataPoint dataPtYLo = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYLo);
				DataPoint dataPtYHi = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYHi);
				float height = dataPtYLo.getHeight() * iYLoP + dataPtYHi.getHeight() * iYHiP;
				Vector3f normal;
				if (iYLoP >= .5f)
				{
					normal = dataPtYLo.getNormal();
				}
				else
				{
					normal = dataPtYHi.getNormal();
				}
				return new Info(height, normal);
			}
		}
		else if (iYLo == iYHi)
		{
			DataPoint dataPtXLo = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYLo);
			DataPoint dataPtXHi = mHeightMap.getDataPoint(mHeight, mDetail, iXHi, iYLo);
			float height = dataPtXLo.getHeight() * iXLoP + dataPtXHi.getHeight() * iXHiP;
			Vector3f normal;
			if (iXLoP >= .5f)
			{
				normal = dataPtXLo.getNormal();
			}
			else
			{
				normal = dataPtXHi.getNormal();
			}
			return new Info(height, normal);
		}
		else
		{
			DataPoint dataPt[] = new DataPoint[4];
			dataPt[0] = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYLo);
			dataPt[1] = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYHi);
			dataPt[2] = mHeightMap.getDataPoint(mHeight, mDetail, iXHi, iYLo);
			dataPt[3] = mHeightMap.getDataPoint(mHeight, mDetail, iXHi, iYHi);
			float percent[] = new float[4];
			percent[0] = (iXLoP + iYLoP) / 2;
			percent[1] = (iXLoP + iYHiP) / 2;
			percent[2] = (iXHiP + iYLoP) / 2;
			percent[3] = (iXHiP + iYHiP) / 2;
			float height = (dataPt[0].getHeight() * percent[0] + dataPt[1].getHeight() * percent[1]
					+ dataPt[2].getHeight() * percent[2] + dataPt[3].getHeight() * percent[3]) / 4;
			Vector3f normal = new Vector3f();
			for (int i = 0; i < 4; i++)
			{
				Vector3f tmp = new Vector3f(dataPt[i].getNormal()).multiply(percent[i]);
				normal.add(tmp);
			}
			normal.divide(4f);
			return new Info(height, normal);
		}
	}

	void init(float roughness, int detail, long seed)
	{
		mDetail = detail;

		int numX = (int) FloatMath.ceil(mBounds.getSizeX() * detail);
		int numY = (int) FloatMath.ceil(mBounds.getSizeY() * detail);

		mHeightMap = new HeightMap(numX, numY, seed);
		mHeightMap.calc(roughness);
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

}
