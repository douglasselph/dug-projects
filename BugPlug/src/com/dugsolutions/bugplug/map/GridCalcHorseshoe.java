package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.image.TextureManager.Texture;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.terrain.CalcEdgeJagged;
import com.dugsolutions.jacket.terrain.CalcHeightColor;
import com.dugsolutions.jacket.terrain.ICalcValue;
import com.dugsolutions.jacket.terrain.ModelGrid;

public class GridCalcHorseshoe implements IGridHorseshoe
{
	final float	FUDGE				= 0.01f;

	Bounds2D	mBox;
	ModelGrid	mGround;
	ICalcValue	mCalcGround;
	ICalcValue	mCalcWater;

	final float	mMountainHeight;
	final float	mMountainLeftRatio	= .9f;
	final float	mMountainLength		= 10f;
	final float	mMountainRidgeSize	= 2f;
	ModelGrid	mWater;
	final float	mWaterHeight		= 2f;
	final int	mWaterMajorPts		= 10;
	final long	mWaterSeed			= 1;
	final float	mWaterVariance		= 0.5f;

	public GridCalcHorseshoe(boolean hasLight, final Bounds2D bounds, float mountainHeight, Texture ground,
			Texture water, CalcHeightColor calcHeightColor)
	{
		mGround = new ModelGrid();
		mGround.setWithNormals(hasLight);
		mMountainHeight = mountainHeight;
		mBox = bounds;

		mGround.setBounds(mBox).setGridSize(100, 100);
		mGround.setTexture(ground);

		if (hasLight)
		{
			mGround.setColorAmbient(new Color4f(0.2f, 0.2f, 0.2f, 1f));
			mGround.setColorDiffuse(new Color4f(0.4f, 0.4f, 0.4f, 1f));
			mGround.setColorSpecular(new Color4f(0.9f, 0.9f, 0.9f, 1f));
		}
		float xmin = mBox.getMinX();
		float xmax = mBox.getMaxX();
		float ymax = mBox.getMaxY();
		float ymin = ymax - mMountainLength;

		Bounds2D range = new Bounds2D(xmin, ymin, xmax, ymax);
		mCalcGround = new CalcMountainRidge(mMountainHeight, mMountainRidgeSize, mMountainLeftRatio, range);

		if (!hasLight)
		{
			mGround.setComputeColor(calcHeightColor);
		}
		mGround.calc(mCalcGround);
		/*
		 * Build the water edge
		 */
		xmin = mBox.getMinX();
		xmax = mBox.getMaxX();
		ymin = mBox.getMinY();
		ymax = ymin + mWaterHeight;
		range = new Bounds2D(xmin, ymin, xmax, ymax);
		Bounds2D edge = new Bounds2D(range.getMinX() - FUDGE, range.getMaxY() - FUDGE, range.getMaxX() + FUDGE,
				range.getMaxY() + FUDGE);

		CalcEdgeJagged jagged = new CalcEdgeJagged(edge, mWaterSeed);
		jagged.addJag(mWaterMajorPts, mWaterVariance);
		jagged.addJag(mWaterMajorPts * 5, mWaterVariance / 4f);

		mWater = new ModelGrid();
		mWater.setWithNormals(hasLight);
		mWater.setBounds(range).setGridSize(2, jagged.getMaxJagPts());
		mWater.setTexture(water);

		mCalcWater = jagged;

		if (hasLight)
		{
			mWater.setColorAmbient(Color4f.WHITE);
			mWater.setColorDiffuse(Color4f.WHITE);
		}
		mWater.calc(mCalcWater);
	}

	public Bounds2D getBounds()
	{
		return mBox;
	}

	@Override
	public ModelGrid getGround()
	{
		return mGround;
	}

	@Override
	public ModelGrid getWater()
	{
		return mWater;
	}

	@Override
	public void onDraw(GL10 gl)
	{
		mGround.onDraw(gl);
		gl.glTranslatef(0, 0, 0.1f);
		mWater.onDraw(gl);
	}

}
