package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

import com.dugsolutions.bugplug.R;
import com.dugsolutions.jacket.image.ImageUtils;
import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.image.TextureManager.Texture;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.MaterialColors;
import com.dugsolutions.jacket.terrain.CalcHeightColor;

public class Map
{
	static final String		TAG				= "Map";

	final boolean			mHasLight;
	final TextureManager	mTM;
	IGridHorseshoe			mGridHorseshoe;

	final float				mHeight			= 13f;
	final float				mWidth			= 11f;

	final float				mMountainHeight	= 1.0f;

	final Color4f			mColorMax		= new Color4f(1f, 1f, 1f);
	final float				mColorMaxHeight	= mMountainHeight;
	final float				mColorMinV		= 0.50f;
	final Color4f			mColorMin		= new Color4f(mColorMinV, mColorMinV, mColorMinV);
	final float				mColorMinHeight	= -mMountainHeight / 4;

	final int				mGroundTerrain	= R.drawable.dirt;
	final boolean			mLoadTerrain	= true;

	CalcHeightColor			mCalcHeightColor;
	final Bounds2D			mBounds;

	public Map(TextureManager tm, boolean hasLight)
	{
		mTM = tm;
		mHasLight = hasLight;

		mBounds = new Bounds2D(-mWidth / 2, -mHeight / 2, mWidth / 2, mHeight / 2);
		mCalcHeightColor = new CalcHeightColor(mColorMinHeight, mColorMaxHeight, mColorMin, mColorMax);

		Texture ground = mTM.getTexture(mGroundTerrain);

		if (mLoadTerrain)
		{
			Bitmap heightMap = ImageUtils.LoadBitmap(tm.getContext(), R.drawable.horseshoe);
			mGridHorseshoe = new GridLoadHorseshoe(heightMap, mBounds, mMountainHeight * 2, ground, mCalcHeightColor);
		}
		else
		{
			mGridHorseshoe = new GridCalcHorseshoe(hasLight, mBounds, mMountainHeight, ground,
					mTM.getTexture(R.drawable.water), mCalcHeightColor);
		}
	}

	public Bounds2D getBounds()
	{
		return mBounds;
	}

	public MaterialColors getGroundMatColors()
	{
		return mGridHorseshoe.getGround().getMatColors();
	}

	public MaterialColors getWaterMatColors()
	{
		return mGridHorseshoe.getWater().getMatColors();
	}

	public void onDraw(GL10 gl)
	{
		mGridHorseshoe.onDraw(gl);
	}
}
