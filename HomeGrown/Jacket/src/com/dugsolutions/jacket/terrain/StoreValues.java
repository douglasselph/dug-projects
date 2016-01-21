package com.dugsolutions.jacket.terrain;

public class StoreValues
{
	int		mNumCols;
	int		mNumRows;
	float[]	mValues;

	public StoreValues(int numRows, int numCols)
	{
		mNumRows = numRows;
		mNumCols = numCols;
		mValues = new float[mNumRows * mNumCols];
	}

	public float get(int x, int y)
	{
		return mValues[getIndex(x, y)];
	}

	int getIndex(int x, int y)
	{
		return y * mNumCols + x;
	}

	public void put(int x, int y, float val)
	{
		mValues[getIndex(x, y)] = val;
	}

	public boolean within(int x, int y)
	{
		return (y >= 0 && x >= 0 && y < mNumRows && x < mNumCols);
	}
}
