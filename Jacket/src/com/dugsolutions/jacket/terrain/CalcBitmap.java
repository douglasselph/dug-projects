package com.dugsolutions.jacket.terrain;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.dugsolutions.jacket.math.Bounds2D;

public class CalcBitmap extends CalcConstant
{
	Bitmap	mBitmap;
	float	mXFactor;
	float	mYFactor;

	public CalcBitmap(Bitmap heightMap, float maxHeight, Bounds2D bounds)
	{
		super(maxHeight, bounds);
		mBitmap = heightMap;
		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			int ix = getCol(x);
			int iy = getRow(y);
			if (within(ix, iy))
			{
				int col = mBitmap.getPixel(ix, iy);
				int red = Color.red(col);
				float percent = (float) red / (float) 0xFF;
				float height = percent * mHeight;
				info.setHeight(height);

				if (info.genNormal())
				{
				}
			}
		}
	}

	int getCol(float x)
	{
		float bx = (x - mBounds.getMinX()) * mXFactor;
		return (int) Math.round(bx);
	}

	public int getNumCols()
	{
		return mBitmap.getWidth();
	}

	public int getNumRows()
	{
		return mBitmap.getHeight();
	}

	int getRow(float y)
	{
		float by = (y - mBounds.getMinY()) * mYFactor;
		return (int) Math.round(by);
	}

	void init()
	{
		if (mHeight > 0)
		{
			mXFactor = (float) mBitmap.getWidth() / mBounds.getSizeX();
			mYFactor = (float) mBitmap.getHeight() / mBounds.getSizeY();
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}

	boolean within(int x, int y)
	{
		return (x >= 0 && x < mBitmap.getWidth() && y >= 0 && y < mBitmap.getHeight());
	}
}
