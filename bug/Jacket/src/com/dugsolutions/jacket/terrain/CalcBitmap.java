package com.dugsolutions.jacket.terrain;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.dugsolutions.jacket.math.Bounds2D;

public class CalcBitmap extends CalcValue
{
	Bitmap			mBitmap;
	int				mBitmapMaxVal;
	int				mBitmapMinVal;
	int				mBitmapValRange;
	final boolean	mDoScale;
	final float		mHeightMax;
	final float		mHeightMin;
	float			mHeightSize;
	float			mXFactor;
	float			mYFactor;

	public CalcBitmap(Bitmap heightMap, float minHeight, float maxHeight, boolean doScale, Bounds2D bounds)
	{
		super(bounds);
		mBitmap = heightMap;
		mHeightMax = maxHeight;
		mHeightMin = minHeight;
		mDoScale = doScale;
		init();
	}

	public void cleanup()
	{
		mBitmap.recycle();
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
				float percent;
				if (mDoScale)
				{
					percent = ((float) (red - mBitmapMinVal) / mBitmapValRange);
				}
				else
				{
					percent = (float) red / (float) 0xFF;
				}
				float height = percent * mHeightSize + mHeightMin;
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
		float by = mBitmap.getHeight() - 1 - ((y - mBounds.getMinY()) * mYFactor);
		return (int) Math.round(by);
	}

	void init()
	{
		if (mHeightMax > 0)
		{
			mXFactor = (float) (mBitmap.getWidth() - 1) / mBounds.getSizeX();
			mYFactor = (float) (mBitmap.getHeight() - 1) / mBounds.getSizeY();
			mHeightSize = mHeightMax - mHeightMin;

			if (mDoScale)
			{
				int col;
				int red;

				col = mBitmap.getPixel(0, 0);
				red = Color.red(col);
				mBitmapMinVal = red;
				mBitmapMaxVal = red;

				for (int y = 0; y < mBitmap.getHeight(); y++)
				{
					for (int x = 0; x < mBitmap.getWidth(); x++)
					{
						col = mBitmap.getPixel(x, y);
						red = Color.red(col);
						if (red < mBitmapMinVal)
						{
							mBitmapMinVal = red;
						}
						else if (red > mBitmapMaxVal)
						{
							mBitmapMaxVal = red;
						}
					}
				}
				mBitmapValRange = mBitmapMaxVal - mBitmapMinVal;
			}
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