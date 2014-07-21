package com.tipsolutions.panda.data;

import java.util.Arrays;

public class CellMap
{
	int		mNumX;
	int		mNumY;
	byte[]	mMap;

	public CellMap(byte[] data, int width) throws Exception
	{
		mMap = data;
		mNumX = width;
		mNumY = (data.length * 8) / width;

		int expectedsize = mNumX * mNumY;

		if (expectedsize != mMap.length * 8)
		{
			throw new Exception("Expected data size of " + expectedsize + ", found " + mMap.length * 8
					+ ". Data length=" + mMap.length + ", Data width=" + mNumX + ", Num Rows=" + mNumY);
		}
	}

	public CellMap(int numx, int numy)
	{
		mNumX = numx;
		mNumY = numy;
		int size = mNumX * mNumY;
		int size8 = size / 8 + 1;
		mMap = new byte[size8];
	}

	void clear()
	{
		byte val = 0;
		Arrays.fill(mMap, val);
	}

	void clear(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		mMap[byteP] &= ~val;
	}

	public int getHeight()
	{
		return mNumY;
	}

	public int getWidth()
	{
		return mNumX;
	}

	public boolean is(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		return (mMap[byteP] & val) == val;
	}

	public void overlap(CellMap map, int offsetx, int offsety)
	{
		int x, y;
		int endx, endy;

		if (map.getHeight() + offsety >= getHeight())
		{
			endy = getHeight() - ((map.getHeight() + offsety) - getHeight());
		}
		else
		{
			endy = getHeight();
		}
		if (map.getWidth() + offsetx >= getWidth())
		{
			endx = getWidth() - ((map.getWidth() + offsetx) - getWidth());
		}
		else
		{
			endx = getWidth();
		}
		for (y = 0; y < endy; y++)
		{
			for (x = 0; x < endx; x++)
			{
				if (map.is(x, y))
				{
					map.set(x + offsetx, y + offsety);
				}
			}
		}
	}

	void set(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		mMap[byteP] |= val;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[NUMX=");
		sbuf.append(mNumX);
		sbuf.append(", NUMY=");
		sbuf.append(mNumY);
		sbuf.append("\n");
		for (int y = 0; y < mNumY; y++)
		{
			for (int x = 0; x < mNumX; x++)
			{
				sbuf.append(is(x, y) ? "1" : "0");
			}
			sbuf.append("\n");
		}
		return sbuf.toString();
	}

	public boolean within(int x, int y)
	{
		return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
	}

}
