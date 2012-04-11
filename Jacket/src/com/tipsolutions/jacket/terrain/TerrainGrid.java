package com.tipsolutions.jacket.terrain;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.BufUtils;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.model.Model;

public class TerrainGrid extends Model {

	int mNumRows;
	int mNumCols;
	float mWidth;
	float mHeight;
	float mStartX;
	float mStartY;
	ComputeValue mCompute;

	public TerrainGrid(int rows, int columns, float startx, float starty, float width, float height, ComputeValue compute) {
		super();
		
		mNumRows = rows;
		mNumCols = columns;
		mWidth = width;
		mHeight = height;
		mStartX = startx;
		mStartY = starty;
		mCompute = compute;
	}
	
	public void set() {
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
					z = mCompute.getHeight(x, y);
					normal = mCompute.getNormal(x, y);
					if (normal == null) {
						normal = normalDefault;
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
	
	TerrainGrid setGridSize(int nrows, int ncols) {
		mNumRows = nrows;
		mNumCols = ncols;
		return this;
	}
	
	TerrainGrid setStartXY(float x, float y) {
		mStartX = x;
		mStartY = y;
		return this;
	}
	
}
