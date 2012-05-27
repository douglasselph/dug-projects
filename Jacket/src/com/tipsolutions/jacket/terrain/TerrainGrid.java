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
	float mWidth = 1;
	float mHeight = 1;
	float mStartX = 0;
	float mStartY = 0;
	ICalcValue mCompute;

	public TerrainGrid() {
	}

	public ICalcValue getCompute() {
		return mCompute;
	}
	
	public float getHeight() {
		return mHeight;
	}
	
	public int getNumCols() {
		return mNumCols;
	}
	
	public int getNumRows() {
		return mNumRows;
	}
	
	public float getStartX() {
		return mStartX;
	}
	
	public float getStartY() {
		return mStartY;
	}
	
	public float getWidth() {
		return mWidth;
	}
	
	public void init() {
		int numVertex = (mNumRows+1)*(mNumCols+1);

		FloatBuffer vbuf = BufUtils.setSize(mVertexBuf, numVertex*3);
		FloatBuffer nbuf = BufUtils.setSize(mNormalBuf, numVertex*3);
		ShortBuffer sbuf = BufUtils.setSize(mIndexBuf, (mNumCols+1)*2*mNumRows);
		FloatBuffer tbuf = BufUtils.setSize(mTextureBuf, numVertex*2);

		vbuf.rewind();
		nbuf.rewind();
		sbuf.rewind();
		tbuf.rewind();

		Vector3f normalDefault = new Vector3f(0, 0, 1);
		Vector3f normal = normalDefault;
		float incX = mWidth / mNumCols;
		float incY = mHeight / mNumRows;
		float y = mStartY;
		float x;
		float z = 0;
		float percentX;
		float percentY = 0;
		float percentIncX = 1f/mNumCols;
		float percentIncY = 1f/mNumRows;
		short index;
		short indexRowInc = (short)(mNumCols+1);

		for (int row = 0; row <= mNumRows; row++) {
			x = mStartX;
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

	public TerrainGrid setDimension(float width, float height) {
		mWidth = width;
		mHeight = height;
		return this;
	}
	
	public TerrainGrid setGranularity(int nrows, int ncols) {
		mNumRows = (int) (mHeight * nrows);
		mNumCols = (int) (mWidth * ncols);
		return this;
	}
	
	public TerrainGrid setGridSize(int nrows, int ncols) {
		mNumRows = nrows;
		mNumCols = ncols;
		return this;
	}

	public TerrainGrid setStartXY(float x, float y) {
		mStartX = x;
		mStartY = y;
		return this;
	}

}
