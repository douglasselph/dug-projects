package com.tipsolutions.jacket.terrain;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Model;

/**
 * For a single terrain image, supports an arbitrarily complex geometry using
 * generators.
 */
public class TerrainGrid extends Model
{
	Grid	mGrid;

	public TerrainGrid()
	{
		mGrid = new Grid(10, 10);
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
		mVertexBuf.rewind();
		mNormalBuf.rewind();
		mTextureBuf.rewind();
		mIndexBuf.rewind();

		return this;
	}

	public TerrainGrid setCompute(ICalcValue calc)
	{
		mGrid.setCompute(calc);
		return this;
	}

	public TerrainGrid setBounds(Bounds2D bounds)
	{
		mGrid.setBounds(bounds);
		return this;
	}

	public TerrainGrid setGridSize(int nrows, int ncols)
	{
		mGrid.setSize(nrows, ncols);
		return this;
	}

	@Override
	protected void onDrawPre(MatrixTrackingGL gl)
	{
		super.onDrawPre(gl);
		gl.glFrontFace(GL10.GL_CW);
	}

}
