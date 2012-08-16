package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.terrain.CalcConstant;
import com.tipsolutions.jacket.terrain.TerrainGrid;

public class Map
{
	static final String	TAG				= "Map";

	TerrainGrid			mGround;
	TerrainGrid			mWater;
	TerrainGrid[]		mMountains;
	TextureManager		mTM;
	Bounds2D			mBounds;
	final float			mWidth			= 10f;
	final float			mHeight			= 13f;
	final float			mWaterHeight	= 2f;
	final float			mMountainWidth	= 0.8f;
	final float			mMountainHeight	= 8f;

	public Map(TextureManager tm)
	{
		mTM = tm;

		// CalcGroup calcGroup;
		// CalcStore calcStore;

		// TerrainGrid grid = new TerrainGrid().setBounds(new Bounds2D(0, 0, 11f, 2f)).setGranularity(10, 10);
		// CalcGroup calcGroup = new CalcGroup();
		// calcGroup.add(new CalcConstant(4f, new Bounds2D(0, 0, 11f, 2f)));
		// calcGroup.add(new CalcLinear(2f, new Bounds2D(3f, 0f, 6f, 2f)));
		// calcGroup.add(new CalcParabola(5f, 0.4f, new Bounds2D(8f, 0f, 11f, 2f)));
		// CalcStore calcStore = new CalcStore(calcGroup);
		// grid.setCompute(calcStore);
		// grid.setTexture(mTM.getTexture(R.drawable.hardrock));
		// grid.init();
		// mTerrainGrids.addGrid(grid);
		//
		// grid = new TerrainGrid()
		// .setBounds(new Bounds2D(0, 2, 2f, 20f))
		// .setGranularity(10, 10);
		// calcGroup = new CalcGroup();
		// calcGroup.add(new CalcConstant(4f, new Bounds2D(0, 0, 11f, 2f)));
		// calcStore = new CalcStore(calcGroup);
		// grid.setCompute(calcStore);
		// grid.setTexture(mTM.getTexture(R.drawable.water));
		// grid.init();
		//
		// grid = new TerrainGrid()
		// .setBounds(new Bounds2D(0, 0, 11f, 20f))
		// .setGranularity(10, 10);
		// calcGroup = new CalcGroup();
		// calcGroup.add(new CalcConstant(1f, new Bounds2D(0, 0, 11f, 20f)));
		// calcGroup.add(new CalcLinear(1f, new Bounds2D(5f, 5f, 10f, 15f)));
		// calcGroup.add(new CalcParabola(1f, 0.4f, new Bounds2D(3f, 16f, 8f,
		// 20f)));
		// calcStore = new CalcStore(calcGroup);
		// grid.setCompute(calcStore);
		// grid.setTexture(mTM.getTexture(R.drawable.dirt));
		// grid.init();

		/*
		 * Define total bounds
		 */
		mBounds = new Bounds2D(-mWidth / 2, -mHeight / 2, mWidth / 2, mHeight / 2);
		Bounds2D bounds;
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

		mWater = new TerrainGrid();
		mWater.setBounds(bounds).setGridSizeSafe(2, 2);
		mWater.setCompute(new CalcConstant(0f, bounds));
		mWater.setTexture(mTM.getTexture(R.drawable.water));
		mWater.init();
		/*
		 * Build the mountains
		 */
		mMountains = new TerrainGrid[3];

		// LEFT
		bounds = new Bounds2D(mBounds.getMinX(), mBounds.getMaxY() - mMountainHeight, mBounds.getMinX()
				+ mMountainWidth, mBounds.getMaxY());

		mMountains[0] = new TerrainGrid(); // left
		mMountains[0].setBounds(bounds).setGridSizeSafe(2, 2);
		mMountains[0].setCompute(new CalcConstant(0f, bounds));
		mMountains[0].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[0].init();

		// TOP
		bounds = new Bounds2D(mBounds.getMinX() + mMountainWidth, mBounds.getMaxY() - mMountainWidth, mBounds.getMaxX()
				- mMountainWidth, mBounds.getMaxY());

		mMountains[1] = new TerrainGrid(); // left
		mMountains[1].setBounds(bounds).setGridSizeSafe(2, 2);
		mMountains[1].setCompute(new CalcConstant(0f, bounds));
		mMountains[1].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[1].init();

		// RIGHT
		bounds = new Bounds2D(mBounds.getMaxX() - mMountainWidth, mBounds.getMaxY() - mMountainHeight,
				mBounds.getMaxX(), mBounds.getMaxY());

		mMountains[2] = new TerrainGrid(); // left
		mMountains[2].setBounds(bounds).setGridSizeSafe(2, 2);
		mMountains[2].setCompute(new CalcConstant(0f, bounds));
		mMountains[2].setTexture(mTM.getTexture(R.drawable.hardrock));
		mMountains[2].init();
	}

	public void onDraw(GL10 gl)
	{
		gl.glTranslatef(0, 0, mBounds.getSizeX() * -1.05f);
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
		return mGround.toString();
	}

}
