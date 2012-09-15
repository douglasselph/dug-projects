package com.dugsolutions.jacket.terrain;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.model.Model;

/**
 * For a single terrain image, supports an arbitrarily complex geometry using
 * generators.
 */
public class ModelGrid extends Model
{
	static final String	TAG	= "ModelGrid";

	Grid				mGrid;

	public ModelGrid()
	{
		mGrid = new Grid(10, 10);
	}

	public void calc(ICalcValue calc)
	{
		mGrid.calc(calc);
		init();
	}

	/**
	 * Calculate the array.
	 * Call setGridSize(), setBounds(), and setCompute() first.
	 * 
	 * @return
	 */
	void init()
	{
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
	}

	@Override
	protected void onDrawPre(GL10 gl)
	{
		super.onDrawPre(gl);
		gl.glFrontFace(GL10.GL_CW);
	}

	public ModelGrid setBounds(Bounds2D bounds)
	{
		mGrid.setBounds(bounds);
		return this;
	}

	public ModelGrid setComputeColor(ICalcColor calc)
	{
		mGrid.setComputeColor(calc);
		return this;
	}

	public ModelGrid setGridSize(int nrows, int ncols)
	{
		mGrid.setSize(nrows, ncols);
		return this;
	}

	public ModelGrid setRepeating(int rowTimes, int colTimes)
	{
		mGrid.setRepeating(rowTimes, colTimes);
		return this;
	}

	public ModelGrid setWithNormals(boolean flag)
	{
		mGrid.setWithNormals(flag);
		return this;
	}

}
