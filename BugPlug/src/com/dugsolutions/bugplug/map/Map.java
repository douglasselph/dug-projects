package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.bugplug.R;
import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.MaterialColors;
import com.dugsolutions.jacket.terrain.CalcEdgeJagged;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcHeightColor;
import com.dugsolutions.jacket.terrain.TerrainGrid;

public class Map
{
	static final String		TAG					= "Map";

	Bounds2D				mBounds;
	TerrainGrid				mGround;
	final boolean			mHasLight;
	final TextureManager	mTM;
	TerrainGrid				mWater;

	final float				FUDGE				= 0.01f;

	final float				mHeight				= 13f;
	final float				mWidth				= 11f;

	final float				mMountainHeight		= 2f;
	final float				mMountainRidgeSize	= 2f;
	final float				mMountainLength		= 8f;
	final float				mMountainLeftRatio	= .9f;

	final Color4f			mColorMax			= new Color4f(1f, 1f, 1f);
	final float				mColorMaxHeight		= mMountainHeight;
	final float				mColorMinV			= 0.4f;											// 0.75f;
	final Color4f			mColorMin			= new Color4f(mColorMinV, mColorMinV, mColorMinV);
	final float				mColorMinHeight		= -mMountainHeight;

	final float				mWaterHeight		= 2f;
	final int				mWaterMajorPts		= 10;
	final long				mWaterSeed			= 1;
	final float				mWaterVariance		= 0.5f;

	// final int mGroundTerrain = R.drawable.dirt;
	final int				mGroundTerrain		= R.drawable.green;

	public Map(TextureManager tm, boolean hasLight)
	{
		mTM = tm;
		mHasLight = hasLight;
		/*
		 * Define total bounds
		 */
		mBounds = new Bounds2D(-mWidth / 2, -mHeight / 2, mWidth / 2, mHeight / 2);
		Bounds2D bounds;
		Bounds2D edge;
		CalcEdgeJagged jagged;
		CalcGroup group;
		CalcHeightColor heightColor;

		/*
		 * Build the base ground
		 */
		mGround = new TerrainGrid(mHasLight);
		mGround.setBounds(mBounds).setGridSizeSafe(100, 100);
		// mGround.setRepeating(2, 2);
		mGround.setTexture(mTM.getTexture(mGroundTerrain));
		// mGround.setSubdivision(0, 1, 2);

		if (mHasLight)
		{
			mGround.setColorAmbient(new Color4f(0.2f, 0.2f, 0.2f, 1f));
			mGround.setColorDiffuse(new Color4f(0.4f, 0.4f, 0.4f, 1f));
			mGround.setColorSpecular(new Color4f(0.9f, 0.9f, 0.9f, 1f));
		}
		float xmin = mBounds.getMinX();
		float xmax = mBounds.getMaxX();
		float ymax = mBounds.getMaxY();
		float ymin = ymax - mMountainLength;
		bounds = new Bounds2D(xmin, ymin, xmax, ymax);
		group = new CalcMountainRidge(mMountainHeight, mMountainRidgeSize, mMountainLeftRatio, bounds);

		// group = new CalcTest(mMountainHeight, new Bounds2D(xmin, ymin, xmax, ymax));

		if (!mHasLight)
		{
			heightColor = new CalcHeightColor(mColorMinHeight, mColorMaxHeight, mColorMin, mColorMax, mBounds);
			group.add(heightColor);
		}
		mGround.setCompute(group);
		mGround.init();
		/*
		 * Build the water edge
		 */
		xmin = mBounds.getMinX();
		xmax = mBounds.getMaxX();
		ymin = mBounds.getMinY();
		ymax = ymin + mWaterHeight;
		bounds = new Bounds2D(xmin, ymin, xmax, ymax);
		edge = new Bounds2D(bounds.getMinX() - FUDGE, bounds.getMaxY() - FUDGE, bounds.getMaxX() + FUDGE,
				bounds.getMaxY() + FUDGE);

		jagged = new CalcEdgeJagged(edge, mWaterSeed);
		jagged.addJag(mWaterMajorPts, mWaterVariance);
		jagged.addJag(mWaterMajorPts * 5, mWaterVariance / 4f);

		mWater = new TerrainGrid(mHasLight);
		mWater.setBounds(bounds).setGridSizeSafe(2, jagged.getMaxJagPts());
		mWater.setTexture(mTM.getTexture(R.drawable.water));
		mWater.setCompute(jagged);

		if (mHasLight)
		{
			mWater.setColorAmbient(Color4f.WHITE);
			mWater.setColorDiffuse(Color4f.WHITE);
		}
		mWater.init();
	}

	public Bounds2D getBounds()
	{
		return mBounds;
	}

	public MaterialColors getGroundMatColors()
	{
		return mGround.getMatColors();
	}

	public MaterialColors getWaterMatColors()
	{
		return mWater.getMatColors();
	}

	public void onDraw(GL10 gl)
	{
		mGround.onDraw(gl);
		gl.glTranslatef(0, 0, 0.1f);
		mWater.onDraw(gl);
	}

	public String toString()
	{
		return "";
	}

}
