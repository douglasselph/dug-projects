package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

import com.dugsolutions.jacket.image.Texture;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.terrain.CalcBitmap;
import com.dugsolutions.jacket.terrain.ICalcColor;
import com.dugsolutions.jacket.terrain.ModelGrid;

public class GridLoadHorseshoe implements IGridHorseshoe
{
	final Bounds2D	mBounds;
	ModelGrid		mGround;

	public GridLoadHorseshoe(Bitmap heightBitmap, final Bounds2D bounds, float mountainHeight, Texture ground,
			ICalcColor calcHeightColor)
	{
		mBounds = bounds;
		mGround = new ModelGrid();
		mGround.setBounds(bounds);
		mGround.setTexture(ground);
		mGround.setComputeColor(calcHeightColor);
		mGround.setWithNormals(false);

		CalcBitmap calcBitmap = new CalcBitmap(heightBitmap, 0f, mountainHeight, true, mBounds);
		mGround.calc(calcBitmap);
	}

	@Override
	public void onDraw(GL10 gl)
	{
		mGround.onDraw(gl);
	}

	@Override
	public ModelGrid getGround()
	{
		return mGround;
	}

	@Override
	public ModelGrid getWater()
	{
		return null;
	}

}
