package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.bugplug.R;
import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.MaterialColors;
import com.dugsolutions.jacket.terrain.CalcConeLinear;
import com.dugsolutions.jacket.terrain.CalcEdgeJagged;
import com.dugsolutions.jacket.terrain.CalcGroup;
import com.dugsolutions.jacket.terrain.CalcMountain;
import com.dugsolutions.jacket.terrain.TerrainGrid;

public class Map
{
	static final String	TAG					= "Map";

	TerrainGrid			mGround;
	TerrainGrid			mWater;
	TextureManager		mTM;
	Bounds2D			mBounds;
	final float			mWidth				= 11f;
	final float			mHeight				= 13f;
	final float			mWaterHeight		= 2f;
	final float			mWaterVariance		= 0.5f;
	final int			mWaterMajorPts		= 10;
	final long			mWaterSeed			= 1;
	final float			mMountainSideXSize	= 2f;
	final float			mMountainTopYSize	= 1.5f;
	final float			mMountainSideYSize	= 8f;
	final float			mMountainHeight		= 2f;
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
		CalcEdgeJagged jagged;
		CalcGroup group;
		CalcConeLinear rise;
		CalcMountain mountain;
		/*
		 * Build the base ground
		 */
		mGround = new TerrainGrid();
		mGround.setBounds(mBounds).setGridSizeSafe(100, 100);
		// mGround.setRepeating(2, 2);
		mGround.setTexture(mTM.getTexture(R.drawable.dirt));
		mGround.setColorAmbient(new Color4f(0.2f, 0.2f, 0.2f, 1f));
		mGround.setColorDiffuse(new Color4f(0.4f, 0.4f, 0.4f, 1f));
		mGround.setColorSpecular(new Color4f(0.9f, 0.9f, 0.9f, 1f));
		mGround.setSubdivision(0, 1, 2);

		group = new CalcGroup();
		// Left rise
		edge = new Bounds2D(mBounds.getMinX(), mBounds.getMaxY() - mMountainSideYSize, mBounds.getMinX()
				+ mMountainSideXSize, mBounds.getMaxY());
		// mountain = new CalcMountain(mMountainHeight, 0.2f, 10, 1L, edge);
		// group.add(mountain);

		// Top rise
		edge = new Bounds2D(mBounds.getMinX(), mBounds.getMaxY() - mMountainTopYSize, mBounds.getMaxX(),
				mBounds.getMaxY());
		mountain = new CalcMountain(mMountainHeight, 0.4f, 10, 1L, edge);
		group.add(mountain);
		// Right rise
		edge = new Bounds2D(mBounds.getMaxX() - mMountainSideXSize, mBounds.getMaxY() - mMountainSideYSize - 1,
				mBounds.getMaxX(), mBounds.getMaxY());
		rise = new CalcConeLinear(mMountainHeight, edge);
		// group.add(rise);

		mGround.setCompute(group);
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
		mWater.setColorAmbient(Color4f.WHITE);
		mWater.setColorDiffuse(Color4f.WHITE);
		mWater.init();
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
	}

	public String toString()
	{
		return "";
	}

	public MaterialColors getGroundMatColors()
	{
		return mGround.getMatColors();
	}

	public MaterialColors getWaterMatColors()
	{
		return mWater.getMatColors();
	}

}
