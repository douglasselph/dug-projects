package com.dugsolutions.jacket.terrain;

import java.util.HashMap;

import android.util.Log;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.MathUtils;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.math.BufUtils.FloatBuf;
import com.dugsolutions.jacket.math.BufUtils.ShortBuf;
import com.dugsolutions.jacket.math.BufUtils.TmpFloatBuf;
import com.dugsolutions.jacket.math.BufUtils.TmpShortBuf;

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
		TmpFloatBuf		mVertexBuf;
		TmpFloatBuf		mNormalBuf;
		TmpFloatBuf		mTexBuf;
		TmpShortBuf		mIndexBuf;
		final Vector3f	mNormalDefault	= new Vector3f(0, 0, 1);
		short			mPosition;

		public BufferResult()
		{
			mVertexBuf = new TmpFloatBuf();
			mNormalBuf = new TmpFloatBuf();
			mTexBuf = new TmpFloatBuf();
			mIndexBuf = new TmpShortBuf();
		}

		public void cleanup()
		{
			mVertexBuf.clear();
			mNormalBuf.clear();
			mTexBuf.clear();
			mIndexBuf.clear();
		}

		public ShortBuf getIndexBuf()
		{
			return mIndexBuf.create();
		}

		short getNextPosition()
		{
			return mPosition;
		}

		public FloatBuf getNormalBuf()
		{
			return mNormalBuf.create();
		}

		public FloatBuf getTexBuf()
		{
			return mTexBuf.create();
		}

		public FloatBuf getVertexBuf()
		{
			return mVertexBuf.create();
		}

		public void put(float x, float y, float percentX, float percentY)
		{
			Vector3f normal;
			float z;
			float xadj;
			float yadj;

			if (mCompute != null)
			{
				Info info = mCompute.getInfo(x, y);
				if (info == null)
				{
					z = 0;
					xadj = 0;
					yadj = 0;
					normal = mNormalDefault;
				}
				else
				{
					z = info.getHeight();
					xadj = info.getXAdjust();
					yadj = info.getYAdjust();
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
				xadj = 0;
				yadj = 0;
			}
			mVertexBuf.put(x + xadj).put(y + yadj).put(z);
			mNormalBuf.put(normal.getX()).put(normal.getY()).put(normal.getZ());
			mTexBuf.put(percentX).put(percentY);
			mPosition++;
		}

		public void put(int id, HashMap<Integer, Short> map, float x, float y, float percentX, float percentY)
		{
			map.put(id, getNextPosition());
			put(x, y, percentX, percentY);
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

	static final String		TAG			= "Grid";

	int						mNumRows;
	int						mNumCols;
	int						mTimesRow	= 1;
	int						mTimesCol	= 1;
	/** Indicates which major cells have sub-divisions */
	byte[]					mSubdivision;
	/** Track index by row,col,subrow,subcol for vertex, normal, and tex arrays */
	HashMap<Integer, Short>	mIndexTL	= new HashMap<Integer, Short>();
	/** On repeating texture maps, need secondary points along edge boundaries */
	/** Top-Right, Column Edge repeats */
	HashMap<Integer, Short>	mIndexTR	= new HashMap<Integer, Short>();
	/** Bottom-Left, Row Edge repeats */
	HashMap<Integer, Short>	mIndexBL	= new HashMap<Integer, Short>();
	/** Bottom-Right Row,Col Edge repeats */
	HashMap<Integer, Short>	mIndexBR	= new HashMap<Integer, Short>();

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
		mResult = new BufferResult();
		calcPoints();
		calcIndexes();
	}

	/**
	 * Calculate all the triangles by filling up the index array.
	 */
	void calcIndexes()
	{
		short indexes[] = new short[4]; // 0: this, 1: right, 2:bottom, 3: bottom-right

		// CW
		if (mSubdivision == null)
		{
			if (!isRepeating())
			{
				short index = 0;

				for (int row = 0; row < mNumRows; row++)
				{
					for (int col = 0; col < mNumCols; col++, index++)
					{
						indexes[0] = index;
						indexes[1] = (short) (index + 1); // right
						indexes[2] = (short) (index + mNumCols + 1); // bottom
						indexes[3] = (short) (indexes[2] + 1); // right bottom

						mResult.putTwoTriangles(indexes);
					}
					index++;
				}
			}
			else
			{
				for (int row = 0; row < mNumRows; row++)
				{
					for (int col = 0; col < mNumCols; col++)
					{
						indexes[0] = mIndexTL.get(ID(row, col)); // top-left
						indexes[1] = getIndex(ID(row, col + 1), mIndexTR); // top-right
						indexes[2] = getIndex(ID(row + 1, col), mIndexBL); // bottom-left
						indexes[3] = getIndex(ID(row + 1, col + 1), mIndexBR, mIndexTR, mIndexBL); // bottom-right

						mResult.putTwoTriangles(indexes);
					}
				}
			}
		}
		else
		{
			int subRow;
			int subCol;
			int numCells;
			short subDivision;
			int rc = 0;

			for (int row = 0; row < mNumRows; row++)
			{
				for (int col = 0; col < mNumCols; col++)
				{
					subDivision = mSubdivision[rc++];

					if (subDivision == 0)
					{
						indexes[0] = mIndexTL.get(ID(row, col));
						indexes[1] = mIndexTL.get(ID(row, col + 1)); // right
						indexes[2] = mIndexTL.get(ID(row + 1, col)); // bottom
						indexes[3] = mIndexTL.get(ID(row + 1, col + 1)); // right bottom

						mResult.putTwoTriangles(indexes);
					}
					else
					{
						numCells = MathUtils.powOf2(subDivision);

						for (subRow = 0; subRow < numCells; subRow++)
						{
							for (subCol = 0; subCol < numCells; subCol++)
							{
								indexes[0] = mIndexTL.get(ID(row, col, numCells, subRow, subCol));
								indexes[1] = mIndexTL.get(ID(row, col, numCells, subRow, subCol + 1)); // right
								indexes[2] = mIndexTL.get(ID(row, col, numCells, subRow + 1, subCol)); // bottom
								indexes[3] = mIndexTL.get(ID(row, col, numCells, subRow + 1, subCol + 1)); // right-bottom

								mResult.putTwoTriangles(indexes);
							}
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
		float percentX2;
		float percentY = 0;
		float percentY2 = 0;
		int percentXNumCols = mNumCols / mTimesCol;
		int percentYNumRows = mNumRows / mTimesRow;
		int percentXCounter;
		int percentYCounter = 0;
		int id;
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
		 * 
		 * Another complication: on the edge of the texture we need to double up the points depending on which side
		 * of the texture needs to be represented. Upper triangles use the lower portion of the texture on the edge,
		 * Lower triangles use the upper portion of the texture on the edge. Even though both points are physically in
		 * the same spot.
		 */
		mIndexTL.clear();
		mIndexTR.clear();
		mIndexBL.clear();
		mIndexBR.clear();

		if (mSubdivision == null)
		{
			if (!isRepeating())
			{
				float percentIncX = 1f / mNumCols;
				float percentIncY = 1f / mNumRows;

				for (int row = 0; row <= mNumRows; row++)
				{
					x = mBounds2D.getMinX();
					percentX = 0;

					for (int col = 0; col <= mNumCols; col++)
					{
						mResult.put(x, y, percentX, percentY);

						x += incX;
						percentX += percentIncX;
					}
					y -= incY;
					percentY += percentIncY;
				}
			}
			else
			{
				for (int row = 0; row <= mNumRows; row++)
				{
					x = mBounds2D.getMinX();

					percentXCounter = 0;
					percentX = 0;
					percentX2 = 0;

					for (int col = 0; col <= mNumCols; col++)
					{
						id = ID(row, col);

						mResult.put(id, mIndexTL, x, y, percentX, percentY);

						if (percentX != percentX2)
						{
							if (percentY != percentY2)
							{
								mResult.put(id, mIndexTR, x, y, percentX2, percentY);
								mResult.put(id, mIndexBR, x, y, percentX2, percentY2);
								mResult.put(id, mIndexBL, x, y, percentX, percentY2);
							}
							else
							{
								mResult.put(id, mIndexTR, x, y, percentX2, percentY);
							}
						}
						else if (percentY2 != percentY)
						{
							mResult.put(id, mIndexBL, x, y, percentX, percentY2);
						}
						x += incX;
						percentXCounter++;

						if (percentXCounter >= percentXNumCols)
						{
							percentXCounter = 0;
							percentX2 = 1f;
							percentX = 0;
						}
						else
						{
							percentX = (float) percentXCounter / percentXNumCols;
							percentX2 = percentX;
						}
					}
					y -= incY;
					percentYCounter++;

					if (percentYCounter >= percentYNumRows)
					{
						percentYCounter = 0;
						percentY = 0;
						percentY2 = 1;
					}
					else
					{
						percentY = (float) percentYCounter / percentYNumRows;
						percentY2 = percentY;
					}
				}
			}
		}
		else
		{
			float subX = 0;
			float subY = 0;
			float subPercentX = 0;
			float subPercentY = 0;
			float subPercentX2;
			float subPercentY2;
			float subIncX;
			float subIncY;
			float percentIncX = 1f / percentXNumCols;
			float percentIncY = 1f / percentYNumRows;
			byte subDivision;
			int subNumCells = 1;
			int rc;

			rc = 0;

			for (int row = 0; row < mNumRows; row++)
			{
				x = mBounds2D.getMinX();

				percentX = 0;
				percentX2 = 0;
				percentXCounter = 0;

				for (int col = 0; col < mNumCols; col++)
				{
					subDivision = mSubdivision[rc++];
					subNumCells = MathUtils.powOf2(subDivision);

					subY = y;
					subPercentY = percentY;
					subPercentY2 = percentY2;

					subIncX = incX / subNumCells;
					subIncY = incY / subNumCells;

					for (int subRow = 0; subRow <= subNumCells; subRow++)
					{
						subX = x;
						subPercentX = percentX;
						subPercentX2 = percentX2;

						for (int subCol = 0; subCol <= subNumCells; subCol++)
						{
							id = ID(row, col, subNumCells, subRow, subCol);

							if (!mIndexTL.containsKey(id))
							{
								mResult.put(id, mIndexTL, subX, subY, subPercentX, subPercentY);
							}
							subX += subIncX;
							subPercentX += (float) subCol / subNumCells * percentIncX;
							subPercentX2 = subPercentX;
						}
						subY -= subIncY;
						subPercentY += (float) subRow / subNumCells * percentIncY;
						subPercentY2 = subPercentY;
					}
					x += incX;
					percentXCounter++;

					if (percentXCounter >= percentXNumCols)
					{
						percentXCounter = 0;
						percentX2 = 1f;
						percentX = 0;
					}
					else
					{
						percentX = (float) percentXCounter / percentXNumCols;
						percentX2 = percentX;
					}
				}
				y -= incY;
				percentYCounter++;

				if (percentYCounter >= percentYNumRows)
				{
					percentYCounter = 0;
					percentY = 0;
					percentY2 = 1;
				}
				else
				{
					percentY = (float) percentYCounter / percentYNumRows;
					percentY2 = percentY;
				}
			}
		}
	}

	public void cleanup()
	{
		mResult.cleanup();
	}

	public void clearSubdivision()
	{
		mSubdivision = null;
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

	short getIndex(int id, HashMap<Integer, Short> map)
	{
		if (map.containsKey(id))
		{
			return map.get(id);
		}
		return mIndexTL.get(id);
	}

	short getIndex(int id, HashMap<Integer, Short> map, HashMap<Integer, Short> map2, HashMap<Integer, Short> map3)
	{
		if (map.containsKey(id))
		{
			return map.get(id);
		}
		if (map2.containsKey(id))
		{
			return map2.get(id);
		}
		if (map3.containsKey(id))
		{
			return map3.get(id);
		}
		return mIndexTL.get(id);
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

	boolean isRepeating()
	{
		return (mTimesRow > 1 || mTimesCol > 1);
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

	/**
	 * Indicate the number of times the pattern is to be repeated across the rows and cols.
	 * 
	 * @param rowTimes
	 * @param colTimes
	 */
	public void setRepeating(int rowTimes, int colTimes)
	{
		mTimesCol = colTimes;
		mTimesRow = rowTimes;
	}

	/**
	 * Set the size of the grid. This will also clear all sub-divisions set.
	 * 
	 * @param nrows
	 *        : max value of 255.
	 * @param ncols
	 *        : max value of 255.
	 * @throws Exception
	 */
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
		mSubdivision = null;
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
		if (row >= 0 && row < mNumRows && col >= 0 && col < mNumCols)
		{
			if (mSubdivision == null)
			{
				mSubdivision = new byte[mNumRows * mNumCols];
			}
			mSubdivision[posSD(row, col)] = (byte) subdivision;
		}
	}

	/**
	 * Calculate the size of the index array
	 * 
	 * @return
	 */
	// int calcNumIndex()
	// {
	// int count;
	// /*
	// * Each cell or subcell has two triangles each for 6 index.
	// */
	// if (mSubdivision == null)
	// {
	// count = mNumRows * mNumCols * 6;
	// }
	// else
	// {
	// int row;
	// int col;
	// int rc = 0;
	// int numSubCells;
	// int subDivision;
	//
	// count = 0;
	// for (row = 0; row < mNumRows; row++)
	// {
	// for (col = 0; col < mNumCols; col++)
	// {
	// subDivision = mSubdivision[rc++];
	// numSubCells = MathUtils.powOf2(subDivision);
	// count += numSubCells * numSubCells * 6;
	// }
	// }
	// }
	// return count;
	// }

	/**
	 * Calculate the number points needed to represent the grid.
	 * 
	 * @return
	 */
	// int calcNumPoints()
	// {
	// int count;
	//
	// if (mSubdivision == null)
	// {
	// count = (mNumRows + 1) * (mNumCols + 1);
	// }
	// else
	// {
	// int rc;
	// int row;
	// int col;
	// int subNumRC = 0;
	// int subNumCells;
	// int subDivision;
	// int subDivisionRight;
	// int subDivisionBottom;
	// int subDivisionDiff;
	//
	// rc = 0;
	// count = 0;
	// /** Account for the points needed by each cell */
	// for (row = 0; row < mNumRows; row++)
	// {
	// for (col = 0; col < mNumCols; col++, rc++)
	// {
	// subDivision = mSubdivision[rc];
	// subNumRC = MathUtils.powOf2(subDivision);
	// subNumCells = subNumRC * subNumRC;
	// count += subNumCells;
	//
	// if (col == mNumCols - 1)
	// {
	// count += subNumRC; // right edge points
	// }
	// else
	// {
	// subDivisionRight = mSubdivision[rc + 1];
	// if (subDivisionRight < subDivision)
	// {
	// subDivisionDiff = subDivision - subDivisionRight;
	// count += MathUtils.powOf2(subDivisionDiff) - 1;
	// }
	// }
	// if (row == mNumRows - 1)
	// {
	// count += subNumRC; // bottom edge points
	// }
	// else
	// {
	// subDivisionBottom = mSubdivision[rc + mNumCols];
	// if (subDivisionBottom < subDivision)
	// {
	// subDivisionDiff = subDivision - subDivisionBottom;
	// count += MathUtils.powOf2(subDivisionDiff) - 1;
	// }
	// }
	// }
	// }
	// // Now bottom-right point
	// count++;
	// }
	// return count;
	// }

}
