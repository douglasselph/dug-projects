package com.dugsolutions.jacket.terrain;

import java.util.Random;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;

public class CalcBumps extends CalcValue
{
	public static class Config
	{
		float	mMaxHeight;
		float	mMinHeight;
		boolean	mNeg;
		long	mSeed;

		public Config(float height, boolean neg, long seed)
		{
			mMaxHeight = height;
			mMinHeight = height;
			mNeg = neg;
			mSeed = seed;
		}

		public Config(float minHeight, float maxHeight, boolean neg, long seed)
		{
			mSeed = seed;
			mNeg = neg;
			mMinHeight = minHeight;
			mMaxHeight = maxHeight;
		}

		public Config setNegativeOkay()
		{
			mNeg = true;
			return this;
		}

		public Config setSeed(long seed)
		{
			mSeed = seed;
			return this;
		}
	};

	static final String		TAG	= "CalcBumps";
	static final Boolean	LOG	= false;

	float					mBumpDistX;
	float					mBumpDistY;
	int						mBumpsPerRow;
	int						mNumRows;
	int						mNumCols;
	Config					mConfig;
	float					mHeightRange;
	Random					mRandom;
	StoreValues				mStore;

	public CalcBumps(float distX, float distY, Config config, Bounds2D bounds)
	{
		super(bounds);

		mConfig = config;
		mBumpDistX = distX;
		mBumpDistY = distY;

		init();
	}

	public CalcBumps(float height, float distX, float distY, long seed, Bounds2D bounds)
	{
		super(bounds);

		mConfig = new Config(height, true, seed);
		mBumpDistX = distX;
		mBumpDistY = distY;

		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			float dX = x - mBounds.getMinX();
			float dY = y - mBounds.getMinY();
			float iX = dX / mBumpDistX;
			float iY = dY / mBumpDistY;
			int iXL = (int) FloatMath.floor(iX);
			int iXH = (int) FloatMath.ceil(iX);
			float pXH = iX - iXL;
			float pXL = 1 - pXH;
			int iYL = (int) FloatMath.floor(iY);
			int iYH = (int) FloatMath.ceil(iY);
			float pYH = iY - iYL;
			float pYL = 1 - pYH;
			float hLL, hLH, hHL, hHH;
			float pLL, pLH, pHL, pHH;

			if (iYL == iYH || !mStore.within(iXL, iYH))
			{
				if (iXL == iXH || !mStore.within(iXH, iYL))
				{
					hLL = mStore.get(iXL, iYL);
					pLL = 1;
					hLH = hHL = hHH = pLH = pHL = pHH = 0;
				}
				else
				{
					hLL = mStore.get(iXL, iYL);
					hHL = mStore.get(iXH, iYL);
					pLL = pXL;
					pHL = pXH;
					hLH = hHH = pLH = pHH = 0;
				}
			}
			else
			{
				if (iXL == iXH)
				{
					hLL = mStore.get(iXL, iYL);
					hLH = mStore.get(iXL, iYH);
					pLL = pYL;
					pLH = pYH;
					hHL = hHH = pHL = pHH = 0;
				}
				else
				{
					hLL = mStore.get(iXL, iYL);
					hLH = mStore.get(iXL, iYH);
					hHL = mStore.get(iXH, iYL);
					hHH = mStore.get(iXH, iYH);
					pLL = pXL * pYL;
					pLH = pXL * pYH;
					pHL = pXH * pYL;
					pHH = pXH * pYH;
				}
			}
			float height = pLL * hLL + pLH * hLH + pHL * hHL + pHH * hHH;

			info.addHeight(height);
		}
	}

	float genHeight()
	{
		if (mHeightRange == 0)
		{
			if (mConfig.mNeg)
			{
				return mRandom.nextFloat() * (mConfig.mMaxHeight * 2) - mConfig.mMaxHeight;
			}
			else
			{
				return mRandom.nextFloat() * mConfig.mMaxHeight;
			}
		}
		else
		{
			float val = mRandom.nextFloat() * mHeightRange + mConfig.mMinHeight;

			if (mConfig.mNeg)
			{
				if (mRandom.nextBoolean())
				{
					return -val;
				}
				else
				{
					return val;
				}
			}
			else
			{
				return val;
			}
		}
	}

	public int getNumCols()
	{
		return mNumCols;
	}

	public int getNumRows()
	{
		return mNumRows;
	}

	protected void init()
	{
		if (mBumpDistX > 0 && mBumpDistY > 0 && mConfig != null)
		{
			mBumpsPerRow = (int) FloatMath.floor(mBounds.getSizeX() / mBumpDistX);
			mRandom = new Random(mConfig.mSeed);

			int numRows = (int) FloatMath.floor(mBounds.getSizeY() / mBumpDistY) + 1;
			int numCols = (int) FloatMath.floor(mBounds.getSizeX() / mBumpDistX) + 1;
			mStore = new StoreValues(numRows, numCols);

			mHeightRange = mConfig.mMaxHeight - mConfig.mMinHeight;

			for (int y = 0; y < numRows; y++)
			{
				for (int x = 0; x < numCols; x++)
				{
					mStore.put(x, y, genHeight());
				}
			}
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}

	public void setHeight(int ix, int iy, float val)
	{
		mStore.put(ix, iy, val);
	}

	public void setHeightX(int ix, float val)
	{
		for (int iy = 0; iy < mNumRows; iy++)
		{
			mStore.put(ix, iy, val);
		}
	}

	public void setHeightX(int ixl, int ixh, float val)
	{
		for (int iy = 0; iy < mNumRows; iy++)
		{
			for (int ix = ixl; ix <= ixh; ix++)
			{
				mStore.put(ix, iy, val);
			}
		}
	}

	public void setHeightY(int iy, float val)
	{
		for (int ix = 0; ix < mNumCols; ix++)
		{
			mStore.put(ix, iy, val);
		}
	}

	public void setHeightY(int iyl, int iyh, float val)
	{
		for (int iy = iyl; iy <= iyh; iy++)
		{
			for (int ix = 0; ix < mNumCols; ix++)
			{
				mStore.put(ix, iy, val);
			}
		}
	}

	// public CalcBumps testValues()
	// {
	// Info info;
	//
	// float xmin = mBounds.getMinX() + mBounds.getSizeX() / 2 - 0.5f;
	// float xmax = mBounds.getMinX() + mBounds.getSizeX() / 2 + 0.5f;
	// float ymin = mBounds.getMinY();
	// float ymax = mBounds.getMaxY();
	// float xint = 0.1f;
	// float yint = mBounds.getSizeY() / 2;
	//
	// for (float y = ymin; y <= ymax; y += yint)
	// {
	// for (float x = xmin; x <= xmax; x += xint)
	// {
	// info = new Info();
	// fillInfo(x, y, info);
	// if (LOG)
	// {
	// Log.d(TAG, "[" + x + ", " + y + "]=" + info.mHeight);
	// }
	// }
	// }
	// return this;
	// }
}
