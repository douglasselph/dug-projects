package com.dugsolutions.bugplug.map;

import java.util.ArrayList;
import java.util.Random;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.CalcBumps;
import com.dugsolutions.jacket.terrain.CalcConstant;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcMound;
import com.dugsolutions.jacket.terrain.IMapData;
import com.dugsolutions.jacket.terrain.PostCalcSmooth;

public class CalcMountainRidge extends CalcGroup
{
	Bounds2D					mBounds;
	final float					mHeight;
	final float					mRidgeSize;
	final float					mRidgeRatio;
	final float					mLowerOffset	= 0.9f;
	Random						mRandom;
	ArrayList<PostCalcSmooth>	mSmooths		= new ArrayList<PostCalcSmooth>();

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
		mSmooths.clear();

		float leftRidgeLength;
		float rightRidgeLength;

		if (mRidgeRatio < 1)
		{
			rightRidgeLength = mBounds.getSizeY();
			leftRidgeLength = rightRidgeLength * mRidgeRatio;
		}
		else
		{
			leftRidgeLength = mBounds.getSizeY();
			rightRidgeLength = leftRidgeLength / mRidgeRatio;
		}
		final float maxBumpHeight = mHeight / 2;
		final float mainRidgeHeight = mHeight - maxBumpHeight;
		float xmin;
		float ymin;
		float xmax;
		float ymax;
		Bounds2D leftEdge;
		Bounds2D rightEdge;
		Bounds2D topEdge;
		CalcBumps.Config config;
		CalcBumps bumps;

		CalcConstant con = new CalcConstant(.1f, mBounds);
		add(con);

		// Left rise
		xmin = mBounds.getMinX();
		xmax = xmin + mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - leftRidgeLength * mLowerOffset;
		leftEdge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, leftEdge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(leftEdge.getSizeX() / 4, leftEdge.getSizeY() / 5, config, leftEdge);
		bumps.setHeightX(0, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(leftEdge.getSizeX() / 16, leftEdge.getSizeY() / 30, config, leftEdge);
		bumps.setHeightX(0, 3, 0);
		add(bumps);

		// Top rise
		xmin = mBounds.getMinX();
		xmax = mBounds.getMaxX();
		ymax = mBounds.getMaxY();
		ymin = ymax - mRidgeSize;
		topEdge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, topEdge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(topEdge.getSizeX() / 5, topEdge.getSizeY() / 4, config, topEdge);
		bumps.setHeightY(0, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(topEdge.getSizeX() / 30, topEdge.getSizeY() / 16, config, topEdge);
		bumps.setHeightY(0, 3, 0);
		add(bumps);

		// Right rise
		xmax = mBounds.getMaxX();
		xmin = xmax - mRidgeSize;
		ymax = mBounds.getMaxY() - mRidgeSize;
		ymin = mBounds.getMaxY() - rightRidgeLength * mLowerOffset;
		rightEdge = new Bounds2D(xmin, ymin, xmax, ymax);
		add(new CalcMound(mainRidgeHeight, rightEdge));

		config = new CalcBumps.Config(maxBumpHeight, false, getSeed());
		bumps = new CalcBumps(rightEdge.getSizeX() / 4, rightEdge.getSizeY() / 5, config, rightEdge);
		bumps.setHeightX(bumps.getNumCols() - 1, 0);
		add(bumps);

		config = new CalcBumps.Config(maxBumpHeight / 5, true, getSeed());
		bumps = new CalcBumps(rightEdge.getSizeX() / 16, rightEdge.getSizeY() / 30, config, rightEdge);
		bumps.setHeightX(bumps.getNumCols() - 4, bumps.getNumCols() - 1, 0);
		add(bumps);

		PostCalcSmooth smooth;
		// float smoothDist = mRidgeSize / 2;
		// xmin = leftEdge.getMinX();
		// xmax = leftEdge.getMaxX();
		// ymax = topEdge.getMinY() - topEdge.getSizeY() / 2;
		// ymin = ymax - topEdge.getSizeY() / 2;
		// smooth = new PostCalcSmooth(PostCalcSmooth.Orientation.Horizontal, new Bounds2D(xmin, ymin, xmax, ymax));
		// mSmooths.add(smooth);

		xmin = leftEdge.getMinX();
		xmax = leftEdge.getMaxX();
		ymax = leftEdge.getMinY() + mRidgeSize * 2f / 3f;
		ymin = mBounds.getMinY();
		smooth = new PostCalcSmooth(PostCalcSmooth.Orientation.Horizontal, new Bounds2D(xmin, ymin, xmax, ymax));
		mSmooths.add(smooth);
	}

	long getSeed()
	{
		return mRandom.nextLong();
	}

	@Override
	public void postCalc(IMapData map)
	{
		for (PostCalcSmooth smooth : mSmooths)
		{
			smooth.run(map);
		}
	}
}
