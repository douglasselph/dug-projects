package com.tipsolutions.jacket.terrain;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import android.util.Log;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.BufUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufUtils.ShortBuf;
import com.tipsolutions.jacket.math.MathUtils;
import com.tipsolutions.jacket.math.Vector3f;

/**
 * Height map which supports an arbitrarily complex geometry using generators.
 * <p>
 * Generates vertex, normal and tex arrays for all points in the grid.
 * <p>
 * Index generation uses full triangles.
 */
public class Grid
{
	class BufferResult
	{
		FloatBuf		mVertexBuf;
		FloatBuf		mNormalBuf;
		FloatBuf		mTexBuf;
		ShortBuf		mIndexBuf;
		final Vector3f	mNormalDefault	= new Vector3f(0, 0, 1);
		FloatBuffer		mVBuf;
		FloatBuffer		mNBuf;
		FloatBuffer		mTBuf;
		ShortBuffer		mIBuf;
		short			mPosition;

		public BufferResult(int numVertex, int numIndex)
		{
			mVertexBuf = new FloatBuf(numVertex * 3);
			mNormalBuf = new FloatBuf(numVertex * 3);
			mTexBuf = new FloatBuf(numVertex * 2);
			mIndexBuf = new ShortBuf(numIndex);
			mVBuf = mVertexBuf.getBuf();
			mNBuf = mNormalBuf.getBuf();
			mTBuf = mTexBuf.getBuf();
			mIBuf = mIndexBuf.getBuf();
		}

		public ShortBuf getIndexBuf()
		{
			return mIndexBuf;
		}

		short getNextPosition()
		{
			return mPosition;
		}

		public FloatBuf getNormalBuf()
		{
			return mNormalBuf;
		}

		public FloatBuf getTexBuf()
		{
			return mTexBuf;
		}

		public FloatBuf getVertexBuf()
		{
			return mVertexBuf;
		}

		public void put(float x, float y, float percentX, float percentY)
		{
			Vector3f normal;
			float z;

			if (mCompute != null)
			{
				Info info = mCompute.getInfo(x, y);
				if (info == null)
				{
					z = 0;
					normal = mNormalDefault;
				}
				else
				{
					z = info.getHeight();
					normal = info.getNormal();
					if (normal == null)
					{
						normal = mNormalDefault;
					}
				}
			}
			else
			{
				normal = mNormalDefault;
				z = 0;
			}
			mVBuf.put(x).put(y).put(z);

			normal.put(mNBuf);

			mTBuf.put(percentX).put(percentY);

			mPosition++;
		}

		/**
		 * Make two triangles where passed in are four points of a square.
		 * 0: upper-left, 1: upper-right, 2: lower-left, 3: lower-right
		 * 
		 * @param index
		 */
		void putTwoTriangles(short[] index)
		{
			/** Triangle 1 */
			mIndexBuf.put(index[0]);
			mIndexBuf.put(index[1]);
			mIndexBuf.put(index[2]);
			/** Triangle 2 */
			mIndexBuf.put(index[3]);
			mIndexBuf.put(index[2]);
			mIndexBuf.put(index[1]);
		}
	};

	static final String		TAG		= "Grid";
	int						mNumRows;
	int						mNumCols;
	/** Indicates which major cells have sub-divisions */
	byte[]					mSubdivision;
	/** Track index by row,col,subrow,subcol for vertex, normal, and tex arrays */
	HashMap<Integer, Short>	mIndex	= new HashMap<Integer, Short>();
	/** Shared result */
	BufferResult			mResult;
	/** Computational bounds of grid */
	Bounds2D				mBounds2D;
	/** Used to compute the actual values */
	ICalcValue				mCompute;

	/**
	 * 
	 * @param nrows
	 * @param ncols
	 */
	public Grid(int nrows, int ncols)
	{
		try
		{
			setSize(nrows, ncols);
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());

			try
			{
				setSize(10, 10);
			}
			catch (Exception ex2)
			{
			}
		}
	}

	public void calc()
	{
		mResult = new BufferResult(calcNumPoints(), calcNumIndex());
		calcPoints();
		calcIndexes();
	}

	/**
	 * Calculate all the triangles by filling up the index array.
	 */
	void calcIndexes()
	{
		short subDivision;
		short index[] = new short[4]; // 0: this, 1: right, 2:bottom, 3: bottom-right
		int numCells;
		int subRow;
		int subCol;
		int rc = 0;

		// CW
		for (int row = 0; row < mNumRows; row++)
		{
			for (int col = 0; col < mNumCols; col++)
			{
				subDivision = mSubdivision[rc++];

				if (subDivision == 0)
				{
					index[0] = mIndex.get(ID(row, col));
					index[1] = mIndex.get(ID(row, col + 1)); // right
					index[2] = mIndex.get(ID(row + 1, col)); // bottom
					index[3] = mIndex.get(ID(row + 1, col + 1)); // right bottom

					mResult.putTwoTriangles(index);
				}
				else
				{
					numCells = MathUtils.powOf2(subDivision);

					for (subRow = 0; subRow < numCells; subRow++)
					{
						for (subCol = 0; subCol < numCells; subCol++)
						{
							index[0] = mIndex.get(ID(row, col, numCells, subRow, subCol));
							index[1] = mIndex.get(ID(row, col, numCells, subRow, subCol + 1)); // right
							index[2] = mIndex.get(ID(row, col, numCells, subRow + 1, subCol)); // bottom
							index[3] = mIndex.get(ID(row, col, numCells, subRow + 1, subCol + 1)); // right-bottom

							mResult.putTwoTriangles(index);
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param subdivision
	 * @return
	 */
	int calcNumCells(byte subdivision)
	{
		int f1 = MathUtils.powOf2(subdivision);
		return f1 * f1;
	}

	/**
	 * Calculate the size of the index array
	 * 
	 * @return
	 */
	int calcNumIndex()
	{
		int row;
		int col;
		int subDivision;
		int count = 0;
		int rc = 0;
		int numSubCells;
		/*
		 * Each cell or subcell has two triangles for 6 index.
		 */
		for (row = 0; row < mNumRows; row++)
		{
			for (col = 0; col < mNumCols; col++)
			{
				subDivision = mSubdivision[rc++];
				numSubCells = MathUtils.powOf2(subDivision);
				count += numSubCells * numSubCells * 6;
			}
		}
		return count;
	}

	/**
	 * Calculate the number points needed to represent the grid.
	 * 
	 * @return
	 */
	int calcNumPoints()
	{
		int count = 0;
		int rc;
		int row;
		int col;
		int subNumRC = 0;
		int subNumCells;
		int subDivision;
		int subDivisionRight;
		int subDivisionBottom;
		int subDivisionDiff;

		rc = 0;
		/** Account for the points needed by each cell */
		for (row = 0; row < mNumRows; row++)
		{
			for (col = 0; col < mNumCols; col++, rc++)
			{
				subDivision = mSubdivision[rc];
				subNumRC = MathUtils.powOf2(subDivision);
				subNumCells = subNumRC * subNumRC;
				count += subNumCells;

				if (col == mNumCols - 1)
				{
					count += subNumRC; // right edge points
				}
				else
				{
					subDivisionRight = mSubdivision[rc + 1];
					if (subDivisionRight < subDivision)
					{
						subDivisionDiff = subDivision - subDivisionRight;
						count += MathUtils.powOf2(subDivisionDiff) - 1;
					}
				}
				if (row == mNumRows - 1)
				{
					count += subNumRC; // bottom edge points
				}
				else
				{
					subDivisionBottom = mSubdivision[rc + mNumCols];
					if (subDivisionBottom < subDivision)
					{
						subDivisionDiff = subDivision - subDivisionBottom;
						count += MathUtils.powOf2(subDivisionDiff) - 1;
					}
				}
			}

		}
		// Now bottom-right point
		count++;

		return count;
	}

	/**
	 * Calculate out all the points: vertex buffer, normal buffer, and tex buffer.
	 */
	void calcPoints()
	{
		float width = getWidth();
		float height = getHeight();
		float incX = width / mNumCols;
		float incY = height / mNumRows;
		float y = mBounds2D.getMaxY();
		float x;
		float percentX;
		float percentY = 0;
		float percentIncX = 1f / mNumCols;
		float percentIncY = 1f / mNumRows;
		float subX = 0;
		float subY = 0;
		float subPercentX = 0;
		float subPercentY = 0;
		float subIncX;
		float subIncY;
		float subPercentIncX;
		float subPercentIncY;
		byte subDivision;
		int subNumCells = 1;
		int id;
		int rc;
		/*
		 * Do each cell. With no subdivisions, there is one point per cell. With subdivisions, there is a block of
		 * points per cell.
		 * 
		 * Note: the order in which the points are added are important. The index array created later refers to this
		 * order.
		 * 
		 * The right & bottom edge are treated as if it were yet another cell in terms of where the index array value
		 * is, but we only have a single row of points. The subdivision value for these cells share the neighbor edge
		 * cell.
		 */
		mIndex.clear();
		rc = 0;

		for (int row = 0; row < mNumRows; row++)
		{
			x = mBounds2D.getMinX();
			percentX = 0;

			for (int col = 0; col < mNumCols; col++)
			{
				subDivision = mSubdivision[rc++];
				subNumCells = MathUtils.powOf2(subDivision);

				subY = y;
				subPercentY = percentY;
				subIncX = incX / subNumCells;
				subIncY = incY / subNumCells;
				subPercentIncX = percentIncX / subNumCells;
				subPercentIncY = percentIncY / subNumCells;

				for (int subRow = 0; subRow <= subNumCells; subRow++)
				{
					subX = x;
					subPercentX = percentX;

					for (int subCol = 0; subCol <= subNumCells; subCol++)
					{
						id = ID(row, col, subNumCells, subRow, subCol);

						if (!mIndex.containsKey(id))
						{
							mIndex.put(id, mResult.getNextPosition());
							mResult.put(subX, subY, subPercentX, subPercentY);
						}
						subX += subIncX;
						subPercentX += subPercentIncX;
					}
					subY -= subIncY;
					subPercentY += subPercentIncY;
				}
				x += incX;
				percentX += percentIncX;
			}
			y -= incY;
			percentY += percentIncY;
		}
	}

	public void clearSubdivision()
	{
		mSubdivision = new byte[mSubdivision.length];
	}

	public ShortBuf getCalcIndexBuf()
	{
		return mResult.getIndexBuf();
	}

	public FloatBuf getCalcNormalBuf()
	{
		return mResult.getNormalBuf();
	}

	public FloatBuf getCalcTexBuf()
	{
		return mResult.getTexBuf();
	}

	public FloatBuf getCalcVertexBuf()
	{
		return mResult.getVertexBuf();
	}

	public float getHeight()
	{
		return mBounds2D.getSizeY();
	}

	public int getNumCols()
	{
		return mNumCols;
	}

	public int getNumRows()
	{
		return mNumRows;
	}

	public float getWidth()
	{
		return mBounds2D.getSizeX();
	}

	/**
	 * Return a ID that uniquely identifies the position.
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	protected int ID(int row, int col)
	{
		return (row << 24) + (col << 16);
	}

	/**
	 * Return a ID that uniquely identifies the position.
	 * 
	 * @param row
	 * @param col
	 * @param numSubCells
	 * @param subrow
	 * @param subcol
	 * @return
	 */
	protected int ID(int row, int col, int subNumCells, int subrow, int subcol)
	{
		if (subrow == subNumCells)
		{
			row++;
			subrow = 0;
		}
		if (subcol == subNumCells)
		{
			col++;
			subcol = 0;
		}
		return (row << 24) + (col << 16) + (subrow << 8) + subcol;
	}

	/**
	 * Return subDivision index from row,col
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	int posSD(int row, int col)
	{
		return row * mNumCols + col;
	}

	public Grid setBounds(Bounds2D bounds)
	{
		mBounds2D = bounds;
		return this;
	}

	public Grid setCompute(ICalcValue calc)
	{
		mCompute = calc;
		return this;
	}

	public void setSize(int nrows, int ncols) throws Exception
	{
		if (nrows > 255)
		{
			throw new Exception("NRows value too large: " + nrows + ", max value of 255");
		}
		if (ncols > 255)
		{
			throw new Exception("NRows value too large: " + ncols + ", max value of 255");
		}
		mNumRows = nrows;
		mNumCols = ncols;
		mSubdivision = new byte[nrows * ncols];
	}

	/**
	 * Set the subdivision for the indicated cell. By default all cells have no sub-divisions (0). A sub-division of 1,
	 * means the cell is split into 4 cells. A sub-division of 2, means it is split into 16, etc.
	 * 
	 * @param row
	 * @param col
	 * @param subdivision
	 */
	public void setSubdivision(int row, int col, int subdivision)
	{
		int rc = posSD(row, col);
		if (rc >= 0 && rc < mSubdivision.length)
		{
			mSubdivision[rc] = (byte) subdivision;
		}
	}
}
