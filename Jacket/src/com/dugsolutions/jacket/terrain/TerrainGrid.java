package com.dugsolutions.jacket.terrain;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.model.Model;

/**
 * For a single terrain image, supports an arbitrarily complex geometry using
 * generators.
 */
public class TerrainGrid extends Model
{
	static final String	TAG	= "TerrainGrid";

	Grid				mGrid;

	public TerrainGrid(boolean withNormals)
	{
		mGrid = new Grid(10, 10);
		mGrid.setWithNormals(withNormals);
	}

	/**
	 * Calculate the array.
	 * Call setGridSize(), setBounds(), and setCompute() first.
	 * 
	 * @return
	 */
	public TerrainGrid init()
	{
		mGrid.calc();

		mVertexBuf = mGrid.getCalcVertexBuf();
		mNormalBuf = mGrid.getCalcNormalBuf();
		mTextureBuf = mGrid.getCalcTexBuf();
		mIndexBuf = mGrid.getCalcIndexBuf();
		mColorBuf = mGrid.getCalcColorBuf();
		mGrid.cleanup();
		mVertexBuf.rewind();
		mTextureBuf.rewind();
		mIndexBuf.rewind();

		if (mNormalBuf != null)
		{
			mNormalBuf.rewind();
		}
		if (mColorBuf != null)
		{
			mColorBuf.rewind();
		}
		return this;
	}

	@Override
	protected void onDrawPre(GL10 gl)
	{
		super.onDrawPre(gl);
		gl.glFrontFace(GL10.GL_CW);
	}

	public TerrainGrid setBounds(Bounds2D bounds)
	{
		mGrid.setBounds(bounds);
		return this;
	}

	public TerrainGrid setCompute(ICalcValue calc)
	{
		mGrid.setCompute(calc);
		return this;
	}

	public TerrainGrid setComputeColor(ICalcColor calc)
	{
		mGrid.setComputeColor(calc);
		return this;
	}

	public TerrainGrid setGridSize(int nrows, int ncols) throws Exception
	{
		mGrid.setSize(nrows, ncols);
		return this;
	}

	public TerrainGrid setGridSizeSafe(int nrows, int ncols)
	{
		try
		{
			mGrid.setSize(nrows, ncols);
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
		return this;
	}

	public TerrainGrid setRepeating(int rowTimes, int colTimes)
	{
		mGrid.setRepeating(rowTimes, colTimes);
		return this;
	}

	public TerrainGrid setWithNormals(boolean flag)
	{
		mGrid.setWithNormals(flag);
		return this;
	}

}
