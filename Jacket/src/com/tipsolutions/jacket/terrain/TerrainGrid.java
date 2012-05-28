package com.tipsolutions.jacket.terrain;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.BufUtils;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.model.Model;

/**
 * For a single terrain image, supports an arbitrarily complex geometry using
 * generators.
 */
public class TerrainGrid extends Model {

	int mNumRows = 10;
	int mNumCols = 10;
	Bounds2D mBounds2D;
	ICalcValue mCompute;

	public TerrainGrid() {
	}

	public ICalcValue getCompute() {
		return mCompute;
	}
	
	public float getHeight() {
		return mBounds2D.getSizeY();
	}
	
	public int getNumCols() {
		return mNumCols;
	}
	
	public int getNumRows() {
		return mNumRows;
	}
	
	public float getStartX() {
		return mBounds2D.getMinX();
	}
	
	public float getStartY() {
		return mBounds2D.getMinY();
	}
	
	public float getWidth() {
		return mBounds2D.getSizeX();
	}
	
	/**
	 * Warning: setBounds() must be called first.
	 * Then setGridSize(), setGranuality() and setCompute() should be called.
	 */
	public void init() {
		int numVertex = (mNumRows+1)*(mNumCols+1);

		FloatBuffer vbuf = BufUtils.setSize(mVertexBuf, numVertex*3);
		FloatBuffer nbuf = BufUtils.setSize(mNormalBuf, numVertex*3);
		ShortBuffer sbuf = BufUtils.setSize(mIndexBuf, (mNumCols+1)*2*(mNumRows+1));
		FloatBuffer tbuf = BufUtils.setSize(mTextureBuf, numVertex*2);

		vbuf.rewind();
		nbuf.rewind();
		sbuf.rewind();
		tbuf.rewind();

		Vector3f normalDefault = new Vector3f(0, 0, 1);
		Vector3f normal = normalDefault;
		float width = getWidth();
		float height = getHeight();
		float incX = width / mNumCols;
		float incY = height / mNumRows;
		float y = getStartY();
		float x;
		float z = 0;
		float percentX;
		float percentY = 0;
		float percentIncX = 1f/mNumCols;
		float percentIncY = 1f/mNumRows;
		short index;
		short indexRowInc = (short)(mNumCols+1);

		for (int row = 0; row <= mNumRows; row++) {
			x = getStartX();
			percentX = 0;
			index = (short)(row*indexRowInc);

			for (int col = 0; col <= mNumCols; col++) {
				if (mCompute != null) {
					Info info = mCompute.getInfo(x, y);
					if (info == null) {
						z = 0;
						normal = normalDefault;
					} else {
						z = info.getHeight();
						normal = info.getNormal();
						if (normal == null) {
							normal = normalDefault;
						}
					}
				}
				vbuf.put(x).put(y).put(z);

				normal.put(nbuf);

				sbuf.put(index);
				sbuf.put((short)(index+indexRowInc));

				tbuf.put(percentX).put(percentY);

				x += incX;
				percentX += percentIncX;
			}
			y += incY;
			percentY += percentIncY;
		}
		setVertexBuf(vbuf);
		setNormalBuf(nbuf);
		setIndexTriStrip(sbuf, indexRowInc);
		setTextureBuf(tbuf);
	}

	public TerrainGrid setCompute(ICalcValue calc) {
		mCompute = calc;
		return this;
	}

	public TerrainGrid setBounds(Bounds2D bounds) {
		mBounds2D = bounds;
		return this;
	}
	
	/**
	 * Warning: setBounds() must be called first.
	 * 
	 * @param nrows
	 * @param ncols
	 * @return
	 */
	public TerrainGrid setGranularity(int nrows, int ncols) {
		mNumRows = (int) (getHeight() * nrows);
		mNumCols = (int) (getWidth() * ncols);
		return this;
	}
	
	public TerrainGrid setGridSize(int nrows, int ncols) {
		mNumRows = nrows;
		mNumCols = ncols;
		return this;
	}

}
