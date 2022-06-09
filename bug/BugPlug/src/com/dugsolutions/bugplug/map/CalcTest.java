package com.dugsolutions.bugplug.map;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.CalcBumps;
import com.dugsolutions.jacket.terrain.CalcCone;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcMound;

public class CalcTest extends CalcGroup
{
	Bounds2D	mBounds;
	final float	mHeight;

	public CalcTest(float height, Bounds2D bounds)
	{
		super();
		mBounds = bounds;
		mHeight = height;
		init();
	}

	void init()
	{
		final float ySize = mBounds.getSizeY() / 3;
		final float cone1size = ySize - mBounds.getSizeY() / 10;
		final float mound1size = cone1size;
		final float coneHeight = mHeight;
		final float moundHeight = mHeight;

		float xmin = mBounds.getMinX();
		float xmax = xmin + cone1size;
		float ymax = mBounds.getMaxY();
		float ymin = ymax - cone1size;
		Bounds2D bounds = new Bounds2D(xmin, ymin, xmax, ymax);
		CalcCone cone = new CalcCone(coneHeight, bounds);
		add(cone);

		xmin = bounds.getMaxX();
		xmax = mBounds.getMaxX();
		Bounds2D bounds2 = new Bounds2D(xmin, ymin, xmax, ymax);
		cone = new CalcCone(coneHeight, bounds2);
		add(cone);

		xmin = mBounds.getMinX();
		xmax = xmin + mound1size;
		ymax = bounds.getMinY();
		ymin = ymax - mound1size;
		Bounds2D bounds3 = new Bounds2D(xmin, ymin, xmax, ymax);
		CalcMound mound = new CalcMound(moundHeight, bounds3);
		add(mound);

		xmin = bounds3.getMaxX();
		xmax = mBounds.getMaxX();
		Bounds2D bounds4 = new Bounds2D(xmin, ymin, xmax, ymax);
		mound = new CalcMound(moundHeight, bounds4);
		add(mound);

		final float bumpHeight = 0.5f;
		final float bumpSizeX = 0.6f;
		final float bumpSizeY = 0.5f;
		xmin = mBounds.getMinX();
		xmax = mBounds.getMaxX();
		ymax = bounds3.getMinY();
		ymin = mBounds.getMinY();
		Bounds2D bounds5 = new Bounds2D(xmin, ymin, xmax, ymax);
		CalcBumps bumps = new CalcBumps(bumpHeight, bumpSizeX, bumpSizeY, 1, bounds5);
		add(bumps);
	}
}
