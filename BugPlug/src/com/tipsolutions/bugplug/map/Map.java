package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.terrain.CalcConeLinear;
import com.tipsolutions.jacket.terrain.CalcConstant;
import com.tipsolutions.jacket.terrain.CalcEdgeJagged;
import com.tipsolutions.jacket.terrain.CalcEdgeSloped;
import com.tipsolutions.jacket.terrain.CalcGroup;
import com.tipsolutions.jacket.terrain.TerrainGrid;

public class Map
{
	static final String	TAG					= "Map";

	TerrainGrid			mGround;
	TerrainGrid			mWater;
	TerrainGrid[]		mMountains;
	TextureManager		mTM;
	Bounds2D			mBounds;
	final float			mWidth				= 10f;
	final float			mHeight				= 13f;
	final float			mWaterHeight		= 2f;
	final float			mWaterVariance		= 0.5f;
	final int			mWaterMajorPts		= 10;
	final long			mWaterSeed			= 1;
	final float			mMountainSideXSize	= 1f;
	final float			mMountainTopYSize	= 0.8f;
	final float			mMountainSideYSize	= 8f;
	final float			mMountainVariance	= 0.2f;
	final int			mMountainMajorPts	= 20;
	final long			mMountainSeed		= 2;
	final float			mMountainHeight		= 0.6f;
	final float			FUDGE				= 0.01f;

	public Map(TextureManager tm)
	{
		mTM = tm;
		/*
		 * Define total bounds
		 */
		mBounds = new Bounds2D(-mWidth / 2, -mHeight / 2, mWidth / 2, mHeight / 2);
		Bounds2D bounds;
		Bounds2D edge;
		Bounds2D riseBound;
		CalcEdgeJagged jagged;
		CalcGroup group;
		CalcEdgeSloped taper;
		CalcConeLinear rise;
		/*
		 * Build the base ground
		 */
		mGround = new TerrainGrid();
		mGround.setBounds(mBounds).setGridSizeSafe(2, 2);
		mGround.setCompute(new CalcConstant(0f, mBounds));
		mGround.setTexture(mTM.getTexture(R.drawable.sample));
		mGround.setSubdivision(1, 0, 1);
		mGround.setSubdivision(1, 1, 1);
		mGround.setSubdivision(0, 1, 1);
		mGround.init();
		/*
		 * Build the water edge
		 */
		bounds = new Bounds2D(mBounds.getMinX(), mBounds.getMinY(), mBounds.getMaxX(), mBounds.getMinY() + mWaterHeight);
		edge = new Bounds2D(bounds.getMinX() - FUDGE, bounds.getMaxY() - FUDGE, bounds.getMaxX() + FUDGE,
				bounds.getMaxY() + FUDGE);

		jagged = new CalcEdgeJagged(edge, mWaterSeed);
		jagged.addJag(mWaterMajorPts, mWaterVariance);
		jagged.addJag(mWaterMajorPts * 5, mWaterVariance / 4f);

		mWater = new TerrainGrid();
		mWater.setBounds(bounds).setGridSizeSafe(2, jagged.getMaxJagPts());
		mWater.setTexture(mTM.getTexture(R.drawable.water));
		mWater.setCompute(jagged);
		mWater.init();
		/*
		 * Build the mountains
		 */
		mMountains = new TerrainGrid[3];

		// LEFT
		group = new CalcGroup();
		bounds = new Bounds2D(mBounds.getMinX(), mBounds.getMaxY() - mMountainSideYSize, mBounds.getMinX()
				+ mMountainSideXSize, mBounds.getMaxY());
		edge = new Bounds2D(bounds.getMaxX() - FUDGE, bounds.getMinY() - FUDGE, bounds.getMaxX() + FUDGE,
				bounds.getMaxY() - mMountainSideXSize + FUDGE);

		jagged = new CalcEdgeJagged(edge, mMountainSeed);
		jagged.addJag(mMountainMajorPts, mMountainVariance);
		jagged.addJag(mMountainMajorPts * 3, mMountainVariance / 5f);
		group.add(jagged);

		edge = new Bounds2D(bounds.getMinX() + FUDGE, edge.getMinY(), edge.getMaxX(), bounds.getMinY() + 2
				* mMountainSideXSize);
		taper = new CalcEdgeSloped(edge, -mMountainSideXSize * 0.9f, 0f);
		group.add(taper);

		riseBound = new Bounds2D(bounds.getMinX() + mMountainSideXSize / 10f, bounds.getMinY(), bounds.getMaxX()
				- mMountainSideXSize / 10f, bounds.getMaxY());
		rise = new CalcConeLinear(riseBound, mMountainHeight);
		group.add(rise);

		mMountains[0] = new TerrainGrid();
		mMountains[0].setBounds(bounds).setGridSizeSafe(jagged.getMaxJagPts(), 10);
		mMountains[0].setCompute(group);
		mMountains[0].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[0].init();

		// TOP
		bounds = new Bounds2D(mBounds.getMinX() + mMountainSideXSize, mBounds.getMaxY() - mMountainTopYSize,
				mBounds.getMaxX() - mMountainSideXSize, mBounds.getMaxY());
		edge = new Bounds2D(bounds.getMinX() - FUDGE, bounds.getMinY() - FUDGE, bounds.getMaxX() + FUDGE,
				bounds.getMinY() + FUDGE);
		jagged = new CalcEdgeJagged(edge, mMountainSeed + 2);
		jagged.addJag(mMountainMajorPts, mMountainVariance);
		jagged.addJag(mMountainMajorPts * 3, mMountainVariance / 5f);

		mMountains[1] = new TerrainGrid();
		mMountains[1].setBounds(bounds).setGridSizeSafe(2, jagged.getMaxJagPts());
		mMountains[1].setCompute(jagged);
		mMountains[1].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[1].init();

		// RIGHT
		bounds = new Bounds2D(mBounds.getMaxX() - mMountainSideXSize, mBounds.getMaxY() - mMountainSideYSize - 1,
				mBounds.getMaxX(), mBounds.getMaxY());
		edge = new Bounds2D(bounds.getMinX() - FUDGE, bounds.getMinY() - FUDGE, bounds.getMinX() + FUDGE,
				bounds.getMaxY() - mMountainSideXSize);
		jagged = new CalcEdgeJagged(edge, mMountainSeed + 3);
		jagged.addJag(mMountainMajorPts, mMountainVariance);
		jagged.addJag(mMountainMajorPts * 3, mMountainVariance / 5f);

		edge = new Bounds2D(edge.getMinX(), edge.getMinY(), bounds.getMaxX() - FUDGE, bounds.getMinY() + 2
				* mMountainSideXSize);
		taper = new CalcEdgeSloped(edge, mMountainSideXSize * 0.9f, 0f);

		group = new CalcGroup();
		group.add(jagged);
		group.add(taper);

		mMountains[2] = new TerrainGrid();
		mMountains[2].setBounds(bounds).setGridSizeSafe(jagged.getMaxJagPts(), 10);
		mMountains[2].setCompute(group);
		mMountains[2].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[2].init();
	}

	public Bounds2D getBounds()
	{
		return mBounds;
	}

	public void onDraw(GL10 gl)
	{
		mGround.onDraw(gl);
		gl.glTranslatef(0, 0, 0.1f);
		mWater.onDraw(gl);
		for (TerrainGrid grid : mMountains)
		{
			grid.onDraw(gl);
		}
	}

	public String toString()
	{
		return mMountains[0].toString();
	}

}
