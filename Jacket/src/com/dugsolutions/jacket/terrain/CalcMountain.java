package com.dugsolutions.jacket.terrain;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.HeightMap.DataPoint;

/**
 * Calculate a mountain range where the peak is directly in the middle of the boundary defined.
 * 
 * A HeightMap is built using a variation of the diamond/square algorithm.
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

	/**
	 * The 4 height/normals nearest to the incoming x,y is retrieved from the built heightmap. Then a single height and
	 * normal is averaged from this and returned.
	 */
	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float percentX = mBounds.percentX(x);
			float percentY = mBounds.percentY(y);

			float fX = mHeightMap.getPosX(percentX);
			float fY = mHeightMap.getPosY(percentY);
			int iX = (int) Math.round(fX);
			int iY = (int) Math.round(fY);
			DataPoint dataPt = mHeightMap.getDataPoint(mHeight, mDetail, iX, iY, info.genNormal());
			if (dataPt != null)
			{
				info.addHeight(dataPt.getHeight());

				if (info.genNormal())
				{
					info.addNormal(info.getNormal());
				}
			}
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

}
