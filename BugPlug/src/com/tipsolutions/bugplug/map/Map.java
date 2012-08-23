package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.terrain.CalcConeLinear;
import com.tipsolutions.jacket.terrain.CalcEdgeJagged;
import com.tipsolutions.jacket.terrain.CalcGroup;
import com.tipsolutions.jacket.terrain.TerrainGrid;

public class Map
{
	static final String	TAG					= "Map";

	TerrainGrid			mGround;
	TerrainGrid			mWater;
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
	final float			mMountainHeight		= 0.3f;
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
		/*
		 * Build the base ground
		 */
		mGround = new TerrainGrid();
		mGround.setBounds(mBounds).setGridSizeSafe(100, 100);
		mGround.setTexture(mTM.getTexture(R.drawable.dirt));
		mGround.setColorAmbient(new Color4f(0.2f, 0.2f, 0.2f, 1f));
		mGround.setColorDiffuse(new Color4f(0.4f, 0.4f, 0.4f, 1f));
		mGround.setColorSpecular(new Color4f(0.9f, 0.9f, 0.9f, 1f));

		group = new CalcGroup();
		// Left rise
		edge = new Bounds2D(mBounds.getMinX(), mBounds.getMaxY() - mMountainSideYSize, mBounds.getMinX()
				+ mMountainSideXSize, mBounds.getMaxY());
		rise = new CalcConeLinear(edge, mMountainHeight);
		group.add(rise);
		// Top rise
		edge = new Bounds2D(mBounds.getMinX() + mMountainSideXSize, mBounds.getMaxY() - mMountainTopYSize,
				mBounds.getMaxX() - mMountainSideXSize, mBounds.getMaxY());
		rise = new CalcConeLinear(edge, mMountainHeight);
		group.add(rise);
		// Right rise
		edge = new Bounds2D(mBounds.getMaxX() - mMountainSideXSize, mBounds.getMaxY() - mMountainSideYSize - 1,
				mBounds.getMaxX(), mBounds.getMaxY());
		rise = new CalcConeLinear(edge, mMountainHeight);
		group.add(rise);

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

}
