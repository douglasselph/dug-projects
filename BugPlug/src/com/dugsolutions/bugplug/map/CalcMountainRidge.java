package com.dugsolutions.bugplug.map;

import java.util.Random;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.CalcBumps;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcMound;

public class CalcMountainRidge extends CalcGroup
{
	Bounds2D	mBounds;
	final float	mHeight;
	final float	mRidgeSize;
	final float	mRidgeRatio;
	Random		mRandom;

	/**
	 * 
	 * @param bounds
	 * @param height
	 * @param ridgeSize
	 * @param leftRatio
	 *        : the percentage of how much longer the left side is from the right side. If 1, they are equal. If < 1,
	 *        then the left side is shorter than the right. If > 1, then it is longer by that much.
	 */
	public CalcMountainRidge(float height, float ridgeSize, float leftRatio, Bounds2D bounds)
	{
		super();
		mBounds = bounds;
		mHeight = height;
		mRidgeSize = ridgeSize;
		mRidgeRatio = leftRatio;
		mRandom = new Random(16);
		init();
	}

	void init()
	{
		float leftRidgeLength;
		float rightRidgeLength;

		if (mRidgeRatio < 1)
		{
			rightRidgeLength = mBounds.getSizeY();
			leftRidgeLength = mBounds.getSizeY() * mRidgeRatio;
		}
		else
		{
			leftRidgeLength = mBounds.getSizeY();
			rightRidgeLength = mBounds.getSizeY() / mRidgeRatio;
		}
		final float maxBumpHeight = mHeight / 2;
		final float mainRidgeHeight = mHeight - maxBumpHeight;
		float xmin;
		float ymin;
		float xmax;
		float ymax;
		Bounds2D edge;
		CalcBumps.Config config;
		CalcBumps bumps;

		// Left rise
		xmin = mBounds.getMinX();
		xmax = xmin + mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - leftRidgeLength - mRidgeSize;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, edge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 4, edge.getSizeY() / 5, config, edge);
		bumps.setHeightX(0, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 16, edge.getSizeY() / 30, config, edge);
		bumps.setHeightX(0, 3, 0);
		add(bumps);

		// Top rise
		xmin = mBounds.getMinX();
		xmax = mBounds.getMaxX();
		ymax = mBounds.getMaxY();
		ymin = ymax - mRidgeSize;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, edge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 5, edge.getSizeY() / 4, config, edge);
		bumps.setHeightY(0, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 30, edge.getSizeY() / 16, config, edge);
		bumps.setHeightY(0, 3, 0);
		add(bumps);

		// Right rise
		xmax = mBounds.getMaxX();
		xmin = xmax - mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - rightRidgeLength - mRidgeSize;
		edge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, edge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 4, edge.getSizeY() / 5, config, edge);
		bumps.setHeightX(bumps.getNumCols() - 1, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(edge.getSizeX() / 16, edge.getSizeY() / 30, config, edge);
		bumps.setHeightX(bumps.getNumCols() - 4, bumps.getNumCols() - 1, 0);
		add(bumps);
	}

	long getSeed()
	{
		return mRandom.nextLong();
	}
}
