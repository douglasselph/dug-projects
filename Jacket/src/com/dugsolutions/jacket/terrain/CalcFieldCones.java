package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;
import java.util.Random;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

public class CalcFieldCones extends CalcValue
{
	class Cone extends Bounds2D
	{
		final float	mCx;
		final float	mCy;
		final float	mH;
		final float	mMaxDist;

		/**
		 * Square boundary assumed
		 * 
		 * @param bounds
		 * @param h
		 */
		Cone(Bounds2D bounds, float h)
		{
			super(bounds);
			mH = h;
			mCx = getMidX();
			mCy = getMidY();
			mMaxDist = (bounds.getSizeX() + bounds.getSizeY()) / 4;
		}

		void fillInfo(float x, float y, Info info)
		{
			float dX = x - mCx;
			float dY = y - mCy;
			float dist = FloatMath.sqrt(dX * dX + dY * dY);

			if (dist < mMaxDist)
			{
				float h = mH * (1 - (dist / mMaxDist));
				info.addHeight(h);

				if (info.genNormal())
				{
					Vector3f normal;

					if (h > 0)
					{
						normal = new Vector3f(dX, dY, -h);
					}
					else
					{
						normal = new Vector3f(-dX, -dY, h);
					}
					info.addNormal(normal);
				}
			}
		}
	};

	protected ArrayList<Cone>	mCones;
	final protected float		mMaxConeHeight;
	final protected float		mMaxConeSize;
	final protected int			mNumCones;
	final protected Random		mRandom;

	public CalcFieldCones(int numCones, float maxConeHeight, float maxConeSize, long seed, Bounds2D bounds)
	{
		super(bounds);

		mNumCones = numCones;
		mMaxConeHeight = maxConeHeight;
		mMaxConeSize = maxConeSize;
		mRandom = new Random(seed);
		mCones = new ArrayList<Cone>();

		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			for (Cone point : mCones)
			{
				if (point.within(x, y))
				{
					point.fillInfo(x, y, info);
				}
			}
		}
	}

	public void init()
	{
		if (mNumCones == 0)
		{
			return;
		}
		mCones.clear();

		float sX = mBounds.getSizeX();
		float sY = mBounds.getSizeY();
		float mx = mBounds.getMinX();
		float my = mBounds.getMinY();
		Bounds2D bounds;
		float x;
		float y;
		float h;
		float s;

		for (int i = 0; i < mNumCones; i++)
		{
			x = mx + mRandom.nextFloat() * sX;
			y = my + mRandom.nextFloat() * sY;
			h = mRandom.nextFloat() * (mMaxConeHeight * 2) - mMaxConeHeight;
			s = mRandom.nextFloat() * mMaxConeSize;

			bounds = new Bounds2D(x - s, y - s, x + s, y + s);

			mCones.add(new Cone(bounds, h));
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}
}
