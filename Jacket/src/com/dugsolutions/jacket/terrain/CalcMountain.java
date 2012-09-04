package com.dugsolutions.jacket.terrain;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;
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
	public Info getInfo(float x, float y)
	{
		if (!within(x, y))
		{
			return null;
		}
		/*
		 * Strategy: find the percentage within the square the indicated point is. Then, based on that take a calculated
		 * percentage from the four corners to get our final value.
		 */
		float percentX = mBounds.percentX(x);
		float percentY = mBounds.percentY(y);

		float fX = mHeightMap.getPosX(percentX);
		float fY = mHeightMap.getPosY(percentY);
		int iXLo = (int) FloatMath.floor(fX);
		int iXHi = (int) FloatMath.ceil(fX);
		int iYLo = (int) FloatMath.floor(fY);
		int iYHi = (int) FloatMath.ceil(fY);
		float pX = fX - iXLo;
		float pY = fY - iYLo;
		float pXLoYLo;
		float pXHiYLo;
		float pXLoYHi;
		float pXHiYHi;
		float pLeft;

		/* Compute out percentages used from the 4 corners */
		pXHiYHi = pX * pY;
		pXLoYLo = (1 - pX) * (1 - pY);
		pLeft = 1 - pXHiYHi - pXLoYLo;
		pXLoYHi = pLeft * (pY / (pX + pY));
		pXHiYLo = pLeft - pXLoYHi;

		/* Get the four corners */
		DataPoint dataPtXLoYLo = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYLo);
		DataPoint dataPtXLoYHi = mHeightMap.getDataPoint(mHeight, mDetail, iXLo, iYHi);
		DataPoint dataPtXHiYLo = mHeightMap.getDataPoint(mHeight, mDetail, iXHi, iYLo);
		DataPoint dataPtXHiYHi = mHeightMap.getDataPoint(mHeight, mDetail, iXHi, iYHi);

		/* Apply percentages to corner info to get a single result */
		float height = (dataPtXLoYLo.getHeight() * pXLoYLo + dataPtXLoYHi.getHeight() * pXLoYHi
				+ dataPtXHiYLo.getHeight() * pXHiYLo + dataPtXHiYHi.getHeight() * pXHiYHi);
		Vector3f normal = new Vector3f();
		normal.add(new Vector3f(dataPtXLoYLo.getNormal()).multiply(pXLoYLo));
		normal.add(new Vector3f(dataPtXHiYLo.getNormal()).multiply(pXHiYLo));
		normal.add(new Vector3f(dataPtXLoYHi.getNormal()).multiply(pXLoYHi));
		normal.add(new Vector3f(dataPtXHiYHi.getNormal()).multiply(pXHiYHi));
		normal.normalize(); // re-normalize to catch rounding errors

		return new Info(height, normal);
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
