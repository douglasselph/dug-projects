package com.dugsolutions.jacket.terrain;

import java.util.LinkedList;
import java.util.Random;

import com.dugsolutions.jacket.math.Vector3f;

/**
 * A height map is generated using a variation of the square/diamond algorithm.
 * 
 * The result is a generate set of values with the peak value being defined by MAX_HEIGHT.
 * Note that there may be a few values larger than this because of random-variation.
 * 
 * @author dug
 * 
 */
public class HeightMap
{
	class ComputeBox
	{
		final int	mx1;
		final int	mx2;
		final int	my1;
		final int	my2;
		final int	mcx;
		final int	mcy;

		ComputeBox(int x1, int y1, int x2, int y2)
		{
			mx1 = x1;
			my1 = y1;
			mx2 = x2;
			my2 = y2;
			mcx = (mx1 + mx2) / 2;
			mcy = (my1 + my2) / 2;
		}

		void calc()
		{
			int p1 = arrayPos(mx1, my1);
			int p2 = arrayPos(mx2, my1);
			int p3 = arrayPos(mx1, my2);
			int p4 = arrayPos(mx2, my2);
			// Center
			setMid(arrayPos(mcx, mcy), p1, p4);
			// Top
			setMid(arrayPos(mcx, my1), p1, p2);
			// Bottom
			setMid(arrayPos(mcx, my2), p3, p4);
			// Left
			setMid(arrayPos(mx1, mcy), p1, p3);
			// Right
			setMid(arrayPos(mx2, mcy), p2, p4);

			if (mcx != mx1 && mcy != my1)
			{
				mComputeList.add(new ComputeBox(mx1, my1, mcx, mcy));
				mComputeList.add(new ComputeBox(mcx, my1, mx2, mcy));
				mComputeList.add(new ComputeBox(mx1, mcy, mcx, my2));
				mComputeList.add(new ComputeBox(mcx, mcy, mx2, my2));
			}
			else if (mcx != mx1)
			{
				mComputeList.add(new ComputeBox(mx1, my1, mcx, my2));
				mComputeList.add(new ComputeBox(mcx, my1, mx2, my2));
			}
			else if (mcy != my1)
			{
				mComputeList.add(new ComputeBox(mx1, my1, mx2, mcy));
				mComputeList.add(new ComputeBox(mx1, mcy, mx2, my2));
			}
		}

		int getCenterPos()
		{
			return arrayPos(mcx, mcy);
		}

		void setMid(int midpos, int spos, int epos)
		{
			if (getHeight(midpos) == 0)
			{
				int height = (getHeight(spos) + getHeight(epos)) / 2;
				int variance = (getVariance(spos) + getVariance(epos)) / 2;

				if (variance > 0)
				{
					height += getRandom((short) variance);
				}
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

	static final short		MAX_HEIGHT			= 10000;
	static final short		MAX_VARIANCE		= 1000;

	LinkedList<ComputeBox>	mComputeList		= new LinkedList<ComputeBox>();
	short[]					mHeightVals;
	final int				mNumX;
	final int				mNumY;
	Random					mRandom;
	short[]					mVarianceVals;
	int						mZNormalDirection	= -1;

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

	/**
	 * Calculate the height map with the indicated roughness of the terrain.
	 * 
	 * @param roughness
	 *        : A percentage to indicate what percent of the peak height to start the initial variance.
	 *        A value of something like .1 is expected.
	 */
	public void calc(float roughness)
	{
		short variance = (short) (MAX_HEIGHT * roughness);

		mComputeList.clear();

		setHeightAndVariance(arrayPos(0, 0), mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(arrayPos(mNumX - 1, 0), mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(arrayPos(mNumX - 1, mNumY - 1), mRandom.nextInt(3), mRandom.nextInt(4));
		setHeightAndVariance(arrayPos(0, mNumY - 1), mRandom.nextInt(3), mRandom.nextInt(4));

		ComputeBox compute = new ComputeBox(0, 0, mNumX - 1, mNumY - 1);

		setHeightAndVariance(compute.getCenterPos(), MAX_HEIGHT + mRandom.nextInt(4) - 2, variance);

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
		dataPoint.mNormal.setZ(deltaUnit * 2 * mZNormalDirection);
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
		return (float) (mNumX - 1) * percentX;
	}

	public float getPosY(float percentY)
	{
		return (float) (mNumY - 1) * percentY;
	}

	short getRandom(short mMax)
	{
		return (short) (mRandom.nextInt(mMax) - mMax / 2);
	}

	public int getSizeX()
	{
		return mNumX;
	}

	public int getSizeY()
	{
		return mNumY;
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

	void setHeightAndVariance(int pos, int height, int variance)
	{
		mHeightVals[pos] = (short) height;
		mVarianceVals[pos] = (short) variance;
	}

	void setVariance(int pos, int val)
	{
		mVarianceVals[pos] = (short) val;
	}
}
