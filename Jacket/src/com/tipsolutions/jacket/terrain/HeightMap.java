package com.tipsolutions.jacket.terrain;

import java.util.LinkedList;
import java.util.Random;

import com.tipsolutions.jacket.math.Vector3f;

public class HeightMap
{
	class ComputeBox
	{
		int	mx1;
		int	mx2;
		int	my1;
		int	my2;

		ComputeBox(int x1, int y1, int x2, int y2)
		{
			mx1 = x1;
			my1 = y1;
			mx2 = x2;
			my2 = y2;
		}

		void calc()
		{
			int cx = (mx1 + mx2) / 2;
			int cy = (my1 + my2) / 2;
			int p1 = arrayPos(mx1, my1);
			int p2 = arrayPos(mx2, my1);
			int p3 = arrayPos(mx1, my2);
			int p4 = arrayPos(mx2, my2);
			// Center
			setMid(arrayPos(cx, cy), p1, p4);
			// Top
			setMid(arrayPos(cx, my1), p1, p2);
			// Bottom
			setMid(arrayPos(cx, my2), p3, p4);
			// Left
			setMid(arrayPos(mx1, cy), p1, p3);
			// Right
			setMid(arrayPos(mx2, cy), p2, p4);

			if (cx != mx1)
			{
				mComputeList.add(new ComputeBox(mx1, my1, cx, cy));
				mComputeList.add(new ComputeBox(cx, my1, mx2, cy));
			}
			if (cy != my1)
			{
				mComputeList.add(new ComputeBox(mx1, cy, cx, my2));

				if (cx != mx1)
				{
					mComputeList.add(new ComputeBox(cx, cy, mx2, my2));
				}
			}
		}

		void setMid(int midpos, int spos, int epos)
		{
			if (getHeight(midpos) == 0)
			{
				int height = (getHeight(spos) + getHeight(epos)) / 2;
				int variance = (getVariance(spos) + getVariance(epos)) / 2;
				height += getRandom((short) variance);
				setHeight(midpos, height);
				setVariance(midpos, variance);
			}
		}

	}

	public class DataPoint
	{
		float		mHeight;
		Vector3f	mNormal;

		public float getHeight()
		{
			return mHeight;
		}

		public Vector3f getNormal()
		{
			return mNormal;
		}
	}

	static final short		MAX_HEIGHT		= 10000;
	static final short		MAX_VARIANCE	= 1000;

	LinkedList<ComputeBox>	mComputeList	= new LinkedList<ComputeBox>();
	short[]					mHeightVals;
	final int				mNumX;
	final int				mNumY;
	Random					mRandom;
	short[]					mVarianceVals;

	public HeightMap(int numX, int numY, long seed)
	{
		mNumX = numX;
		mNumY = numY;
		mHeightVals = new short[mNumX * mNumY];
		mVarianceVals = new short[mHeightVals.length];
		mRandom = new Random(seed);
	}

	int arrayPos(int x, int y)
	{
		return y * mNumX + x;
	}

	public void calc(float roughness)
	{
		short variance = (short) (MAX_HEIGHT * roughness);

		mComputeList.clear();

		setHeightAndVariance(0, 0, mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(mNumX - 1, 0, mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(mNumX - 1, mNumY - 1, mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(0, mNumY - 1, mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(mNumX / 2, mNumY / 2, MAX_HEIGHT + mRandom.nextInt(4) - 2, variance);

		ComputeBox compute = new ComputeBox(0, 0, mNumX - 1, mNumY - 1);
		compute.calc();

		while (mComputeList.size() > 0)
		{
			compute = mComputeList.removeFirst();
			compute.calc();
		}
	}

	float convertHeight(float maxHeight, short height)
	{
		return (((float) height * maxHeight) / (float) MAX_HEIGHT);
	}

	/**
	 * 
	 * @param maxHeight
	 *        : The max-height or peak value for the entire height map.
	 * @param deltaUnit
	 *        : The distance one unit in x or y represents.
	 * @param ix
	 *        : the x value on the bitmap to get
	 * @param iy
	 *        : the y value on the bitmap to get
	 * @return data point holding height and normal
	 */
	public DataPoint getDataPoint(float maxHeight, float deltaUnit, int ix, int iy)
	{
		final int TOP = 0;
		final int BOTTOM = 1;
		final int LEFT = 2;
		final int RIGHT = 3;

		if (ix < 0 || iy < 0 || ix >= mNumX || iy >= mNumY)
		{
			return null;
		}
		DataPoint dataPoint = new DataPoint();
		float[] heights = new float[4];
		dataPoint.mHeight = convertHeight(maxHeight, getHeight(ix, iy));
		heights[TOP] = convertHeight(maxHeight, getHeightChk(ix, iy - 1));
		heights[BOTTOM] = convertHeight(maxHeight, getHeightChk(ix, iy + 1));
		heights[LEFT] = convertHeight(maxHeight, getHeightChk(ix - 1, iy));
		heights[RIGHT] = convertHeight(maxHeight, getHeightChk(ix + 1, iy));
		dataPoint.mNormal = new Vector3f();
		dataPoint.mNormal.setX(heights[RIGHT] - heights[LEFT]);
		dataPoint.mNormal.setZ(deltaUnit * 2);
		dataPoint.mNormal.setY(heights[TOP] - heights[BOTTOM]);
		dataPoint.mNormal.normalize();
		return dataPoint;
	}

	short getHeight(int pos)
	{
		return mHeightVals[pos];
	}

	short getHeight(int x, int y)
	{
		return mHeightVals[arrayPos(x, y)];
	}

	short getHeightChk(int x, int y)
	{
		if (x >= 0 && x < mNumX && y >= 0 && y < mNumY)
		{
			return getHeight(x, y);
		}
		return 0;
	}

	public float getPosX(float percentX)
	{
		return (float) mNumX * percentX;
	}

	public float getPosY(float percentY)
	{
		return (float) mNumX * percentY;
	}

	short getRandom(short mMax)
	{
		return (short) (mRandom.nextInt(mMax) - mMax / 2);
	}

	short getVariance(int pos)
	{
		return mVarianceVals[pos];
	}

	short getVariance(int x, int y)
	{
		return mVarianceVals[arrayPos(x, y)];
	}

	boolean inBounds(int x, int y)
	{
		if (x >= 0 && x < mNumX && y >= 0 && y < mNumY)
		{
			return true;
		}
		return false;
	}

	void setHeight(int pos, int val)
	{
		mHeightVals[pos] = (short) val;
	}

	void setHeightAndVariance(int x, int y, int height, int variance)
	{
		int pos = arrayPos(x, y);
		mHeightVals[pos] = (short) height;
		mVarianceVals[pos] = (short) variance;
	}

	void setVariance(int pos, int val)
	{
		mVarianceVals[pos] = (short) val;
	}
}
