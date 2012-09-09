package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;
import java.util.Random;

import android.util.FloatMath;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

public class CalcField extends CalcValue
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
				float h = genHeight(dist);
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

		protected float genHeight(float dist)
		{
			return mH * (1 - (dist / mMaxDist));
		}
	}

	class Mound extends Cone
	{
		float	mPA;

		public Mound(Bounds2D bounds, float h)
		{
			super(bounds, h);

			mPA = mH / (getSizeX() / 2);
		}

		@Override
		protected float genHeight(float dist)
		{
			return -mPA * dist * dist + mH;
		}
	}

	protected ArrayList<Cone>	mObjs;
	final protected float		mMaxObjHeight;
	final protected float		mMaxObjSize;
	final protected int			mNumObjs;
	final protected Random		mRandom;
	final protected Shape		mShape;

	public enum Shape
	{
		Cone, Mound;
	}

	public CalcField(Shape shape, int numObjs, float maxObjHeight, float maxObjSize, long seed, Bounds2D bounds)
	{
		super(bounds);

		mShape = shape;
		mNumObjs = numObjs;
		mMaxObjHeight = maxObjHeight;
		mMaxObjSize = maxObjSize;
		mRandom = new Random(seed);
		mObjs = new ArrayList<Cone>();

		init();
	}

	@Override
	public void fillInfo(float x, float y, Info info)
	{
		if (within(x, y))
		{
			for (Cone point : mObjs)
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
		if (mNumObjs == 0)
		{
			return;
		}
		mObjs.clear();

		float sX = mBounds.getSizeX();
		float sY = mBounds.getSizeY();
		float mx = mBounds.getMinX();
		float my = mBounds.getMinY();
		Bounds2D bounds;
		float x;
		float y;
		float h;
		float s;

		for (int i = 0; i < mNumObjs; i++)
		{
			x = mx + mRandom.nextFloat() * sX;
			y = my + mRandom.nextFloat() * sY;
			h = mRandom.nextFloat() * (mMaxObjHeight * 2) - mMaxObjHeight;
			s = mRandom.nextFloat() * mMaxObjSize;

			bounds = new Bounds2D(x - s, y - s, x + s, y + s);

			Cone cone;

			if (mShape == Shape.Mound)
			{
				cone = new Mound(bounds, h);
			}
			else
			{
				cone = new Cone(bounds, h);
			}
			mObjs.add(cone);
		}
	}

	@Override
	public void setBounds(Bounds2D bounds)
	{
		super.setBounds(bounds);
		init();
	}
}
