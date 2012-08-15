package com.tipsolutions.jacket.terrain;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

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
		FloatBuf			mVertexBuf;
		FloatBuf			mNormalBuf;
		FloatBuf			mTexBuf;
		ArrayList<Short>	mIndexArray;
		ShortBuf			mIndexBuf;
		final Vector3f		mNormalDefault	= new Vector3f(0, 0, 1);
		FloatBuffer			mVBuf;
		FloatBuffer			mNBuf;
		FloatBuffer			mTBuf;
		short				mPosition;

		public BufferResult(int numVertex)
		{
			mVertexBuf = new FloatBuf(numVertex * 3);
			mNormalBuf = new FloatBuf(numVertex * 3);
			mTexBuf = new FloatBuf(numVertex * 2);
			mVBuf = mVertexBuf.getBuf();
			mNBuf = mNormalBuf.getBuf();
			mTBuf = mTexBuf.getBuf();
			mIndexArray = new ArrayList<Short>();
		}

		ShortBuf allocIndexBuf()
		{
			ShortBuf buf = new ShortBuf(mIndexArray.size());
			ShortBuffer sbuf = buf.getBuf();
			for (int i = 0; i < mIndexArray.size(); i++)
			{
				sbuf.put(mIndexArray.get(i));
			}
			return buf;
		}

		public ShortBuf getIndexBuf()
		{
			if (mIndexBuf == null)
			{
				mIndexBuf = allocIndexBuf();
			}
			return mIndexBuf;
		}

		public FloatBuf getNormalBuf()
		{
			return mNormalBuf;
		}

		short getPosition()
		{
			return mPosition;
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

			mTBuf.put(percentX).put(1 - percentY);

			mPosition++;
		}

		/**
		 * Make a number of triangles based on the left row of points and two right points.
		 * 
		 * @param leftPts
		 *        : a long list of points.
		 * @param rightPts
		 *        : two points.
		 */
		void putTriangles(short[] leftPts, short[] rightPts)
		{
			int midLeft = leftPts.length / 2;
			int counter = 0;

			// CW

			// To first right point.
			for (; counter < midLeft; counter++)
			{
				mIndexArray.add(leftPts[counter]);
				mIndexArray.add(rightPts[0]);
				mIndexArray.add(leftPts[counter + 1]);
			}
			// Now with second right pt
			for (; counter < leftPts.length - 1; counter++)
			{
				mIndexArray.add(leftPts[counter]);
				mIndexArray.add(rightPts[1]);
				mIndexArray.add(leftPts[counter + 1]);
			}
			// Connecting triangle
			mIndexArray.add(leftPts[midLeft]);
			mIndexArray.add(rightPts[0]);
			mIndexArray.add(rightPts[1]);
		}

		/**
		 * Make two triangles where passed in are four points of a square.
		 * 0: upper-left, 1: upper-right, 2: lower-left, 3: lower-right
		 * 
		 * @param subStartIndex
		 */
		void putTwoTriangles(short[] subStartIndex)
		{
			/** Triangle 1 */
			mIndexArray.add(subStartIndex[0]);
			mIndexArray.add(subStartIndex[1]);
			mIndexArray.add(subStartIndex[2]);
			/** Triangle 2 */
			mIndexArray.add(subStartIndex[3]);
			mIndexArray.add(subStartIndex[2]);
			mIndexArray.add(subStartIndex[1]);
		}
	};

	int				mNumRows;
	int				mNumCols;
	/** Indicates which major cells have sub-divisions */
	byte[]			mSubdivision;
	/** For each cell, starting index for vertex, normal, and tex arrays */
	short[]			mStartIndex;
	/** Shared result */
	BufferResult	mResult;
	/** Computational bounds of grid */
	Bounds2D		mBounds2D;
	/** Used to compute the actual values */
	ICalcValue		mCompute;

	/**
	 * 
	 * @param nrows
	 * @param ncols
	 */
	public Grid(int nrows, int ncols)
	{
		setSize(nrows, ncols);
	}

	public void calc()
	{
		mResult = new BufferResult(calcNumPoints());
		calcVectors();
		calcIndexes();
	}

	/**
	 * Calculate all the triangles by filling up the index array.
	 * 
	 * Note: I am not trying to figure out how many points are in the index array beforehand. Too hard for now. Perhaps
	 * later.
	 */
	void calcIndexes()
	{
		short subDivision[] = new short[3]; // 0: this, 1: right, 2:bottom
		short startIndex[] = new short[4]; // 0: this, 1: right, 2:bottom, 3: bottom-right
		short subStartIndex[] = new short[4]; // 0: this, 1:right, 2: bottom, 3: bottom-right
		int subDivisionPos = -1;
		int startIndexPos = -1;
		int numCells;
		int numCellsRight;
		int numCellsBottom;
		int incExtraRight;
		int incExtraRowsRight;
		int incExtraBottom;
		int incExtraRowsBottom;
		int subRow;
		int subRowRight;
		int subCol;
		int subColBottom;
		int numPts;
		short edgeIndex;
		short ptsLeft[];
		short ptsTop[];
		short ptsRight[] = new short[2];
		short ptsBottom[] = new short[2];
		short subIndexPos;

		// CW

		for (int row = 0; row < mNumRows; row++)
		{
			for (int col = 0; col < mNumCols; col++)
			{
				subDivisionPos++;
				startIndexPos++;

				subDivision[0] = mSubdivision[subDivisionPos];
				subDivision[1] = mSubdivision[subDivisionPos + 1]; // right
				subDivision[2] = mSubdivision[subDivisionPos + mNumCols + 1]; // bottom
				startIndex[0] = mStartIndex[startIndexPos];
				startIndex[1] = mStartIndex[startIndexPos + 1]; // right
				startIndex[2] = mStartIndex[startIndexPos + mNumCols + 1]; // bottom
				startIndex[3] = mStartIndex[startIndexPos + mNumCols + 2]; // right bottom

				if (subDivision[0] == 0)
				{
					mResult.putTwoTriangles(startIndex);
				}
				else
				{
					numCells = MathUtils.powOf2(subDivision[0]);
					numCellsRight = MathUtils.powOf2(subDivision[1]);
					numCellsBottom = MathUtils.powOf2(subDivision[2]);

					subIndexPos = -1;

					// Main triangles
					for (subRow = 0; subRow < numCells - 1; subRow++)
					{
						for (subCol = 0; subCol < numCells - 1; subCol++)
						{
							subIndexPos++;
							subStartIndex[0] = (short) (startIndex[0] + subIndexPos);
							subStartIndex[1] = (short) (subStartIndex[0] + 1);
							subStartIndex[2] = (short) (subStartIndex[0] + numCells);
							subStartIndex[3] = (short) (subStartIndex[2] + 1);

							mResult.putTwoTriangles(subStartIndex);
						}
					}
					//
					// Right Edge
					//
					incExtraRight = subDivision[1] - subDivision[0];

					// Edge triangles
					if (subDivision[0] <= subDivision[1])
					{
						// More or the same cells on right than left
						incExtraRowsRight = incExtraRight * numCellsRight;

						for (subRow = 0; subRow < numCells - 1; subRow++)
						{
							subStartIndex[0] = (short) (startIndex[0] + numCells - 1 + subRow * numCells);
							subStartIndex[1] = (short) (startIndex[1] + (numCellsRight * subRow) + (subRow * incExtraRowsRight));
							subStartIndex[2] = (short) (subStartIndex[0] + numCells * (subRow + 1));
							subStartIndex[3] = (short) (subStartIndex[1] + numCellsRight + incExtraRowsRight);

							mResult.putTwoTriangles(subStartIndex);
						}
					}
					else
					{
						// More cells on left than right is complicated.
						incExtraRowsRight = MathUtils.powOf2(-incExtraRight); // diff converted to #cells
						ptsLeft = new short[incExtraRowsRight];
						edgeIndex = (short) (startIndex[0] + numCells - 1); // first left point

						for (subRowRight = 0; subRowRight < numCellsRight; subRowRight++)
						{
							ptsRight[0] = (short) (startIndex[1] + subRowRight * numCellsRight);

							if (subRowRight == numCellsRight - 1)
							{
								ptsRight[1] = (short) (startIndex[3]);
								// Skip last point on left because it is bottom - way too complicated to consider now.
								// But we will get it later.
								numPts = incExtraRowsRight - 1;
							}
							else
							{
								ptsRight[1] = (short) (ptsRight[0] + numCellsRight);
								numPts = incExtraRowsRight;
							}
							for (subRow = 0; subRow < numPts; subRow++)
							{
								ptsLeft[subRow] = edgeIndex;
								edgeIndex += numCells;
							}
							mResult.putTriangles(ptsLeft, ptsRight);
						}
					}
					//
					// Bottom Edge
					//
					incExtraBottom = subDivision[2] - subDivision[0];

					if (subDivision[0] <= subDivision[2])
					{
						// More or the same cells below than above is not so bad
						for (subCol = 0; subCol < numCells - 1; subCol++)
						{
							subStartIndex[0] = (short) (startIndex[0] + numCells * (numCells - 1) + subCol);
							subStartIndex[1] = (short) (subStartIndex[0] + 1);
							subStartIndex[2] = (short) (startIndex[2] + subCol);
							subStartIndex[3] = (short) (subStartIndex[2] + 1);

							mResult.putTwoTriangles(subStartIndex);
						}
					}
					else
					{
						// More cells above than beneath is complicated.
						incExtraRowsBottom = MathUtils.powOf2(-incExtraBottom); // diff converted to #cells
						ptsTop = new short[incExtraRowsBottom];
						edgeIndex = (short) (startIndex[0] + numCells * numCells - numCells); // first upper point

						for (subColBottom = 0; subColBottom < numCellsBottom; subColBottom++)
						{
							ptsBottom[0] = (short) (startIndex[1] + subColBottom * incExtraRowsBottom);

							if (subColBottom == numCellsBottom - 1)
							{
								ptsBottom[1] = (short) (startIndex[3]);
								numPts = incExtraRowsBottom - 1;
								// Skip last point, way to complicated to figure out, and we already have triangles here
								// anyway.
							}
							else
							{
								ptsBottom[1] = (short) (ptsBottom[0] + 1);
								numPts = incExtraRowsBottom;
							}
							for (subRow = 0; subRow < numPts; subRow++)
							{
								ptsTop[subRow] = edgeIndex;
								edgeIndex += numCells;
							}
							mResult.putTriangles(ptsTop, ptsBottom);
						}
					}
					// Bottom-Right Corner
					subStartIndex[0] = (short) (startIndex[0] + (numCells * numCells) - 1);
					subStartIndex[1] = (short) (startIndex[1] + (numCellsRight * (numCellsRight - 1 - incExtraRight)));
					subStartIndex[2] = (short) (startIndex[2] + numCellsBottom - 1 - incExtraBottom);
					subStartIndex[3] = (short) startIndex[3];

					mResult.putTwoTriangles(subStartIndex);
				}
			}
			startIndexPos++;
			subDivisionPos++;
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

		rc = 0;
		/** Account for the points needed by each cell */
		for (row = 0; row < mNumRows; row++)
		{
			for (col = 0; col < mNumCols; col++)
			{
				subNumRC = MathUtils.powOf2(mSubdivision[rc++]);
				subNumCells = subNumRC * subNumRC;
				count += subNumCells;
			}
			// Add equal number of points as last cell.
			count += subNumRC;
			rc++;
		}
		// Now points to finalize bottom cells
		for (col = 0; col < mNumCols; col++)
		{
			subNumRC = MathUtils.powOf2(mSubdivision[posSD(mNumRows - 1, col)]);
			count += subNumRC;
		}
		// Now bottom-right point
		count++;

		return count;
	}

	/**
	 * Calculate out all the points: vertex buffer, normal buffer, and tex buffer.
	 */
	void calcVectors()
	{
		float width = getWidth();
		float height = getHeight();
		float incX = width / mNumCols;
		float incY = height / mNumRows;
		float y = getStartY();
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
		for (int row = 0; row <= mNumRows; row++)
		{
			x = getStartX();
			percentX = 0;

			for (int col = 0; col <= mNumCols; col++)
			{
				mStartIndex[posSI(row, col)] = mResult.getPosition();

				if (col == mNumCols)
				{
					if (row == mNumRows)
					{
						/* One final point */
						mResult.put(subX, subY, subPercentX, subPercentY);
					}
					else
					{
						/* Borrow subdivision values set on previous column */
						subY = y;
						subX = x;
						subPercentY = percentY;
						subPercentX = percentX;
						subIncY = incY / subNumCells;
						subPercentIncY = percentIncY / subNumCells;

						for (int subRow = 0; subRow < subNumCells; subRow++)
						{
							mResult.put(subX, subY, subPercentX, subPercentY);
							subY += subIncY;
							subPercentY += subPercentIncY;
						}
					}
				}
				else if (row == mNumRows)
				{
					/* Grab subdivision value set on previous row */
					subDivision = mSubdivision[posSD(row - 1, col)];

					if (subDivision > 0)
					{
						subY = y;
						subPercentY = percentY;
						subIncX = incX / subNumCells;
						subPercentIncX = percentIncX / subNumCells;
						subX = x;
						subPercentX = percentX;

						for (int subCol = 0; subCol < subNumCells; subCol++)
						{
							mResult.put(subX, subY, subPercentX, subPercentY);

							subX += subIncX;
							subPercentX += subPercentIncX;
						}
					}
					else
					{
						subNumCells = 1;
						mResult.put(x, y, percentX, percentY);
					}
				}
				else
				{
					subDivision = mSubdivision[posSD(row, col)];

					if (subDivision > 0)
					{
						subNumCells = MathUtils.powOf2(subDivision);

						subY = y;
						subPercentY = percentY;
						subIncX = incX / subNumCells;
						subPercentIncX = percentIncX / subNumCells;
						subPercentIncY = percentIncY / subNumCells;

						for (int subRow = 0; subRow < subNumCells; subRow++)
						{
							subX = x;
							subPercentX = percentX;

							for (int subCol = 0; subCol < subNumCells; subCol++)
							{
								mResult.put(subX, subY, subPercentX, subPercentY);

								subX += subIncX;
								subPercentX += subPercentIncX;
							}
							subY += subIncX;
							subPercentY += subPercentIncY;
						}
					}
					else
					{
						subNumCells = 1;
						mResult.put(x, y, percentX, percentY);
					}
				}
				x += incX;
				percentX += percentIncX;
			}
			y += incY;
			percentY += percentIncY;
		}
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

	public float getStartX()
	{
		return mBounds2D.getMinX();
	}

	public float getStartY()
	{
		return mBounds2D.getMinY();
	}

	public float getWidth()
	{
		return mBounds2D.getSizeX();
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
		return row * (mNumCols + 1) + col;
	}

	/**
	 * Return startIndex index from row,col
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	int posSI(int row, int col)
	{
		return row * (mNumCols + 1) + col;
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

	public void setSize(int nrows, int ncols)
	{
		mNumRows = nrows;
		mNumCols = ncols;

		int size = (nrows + 1) * (ncols + 1);
		mSubdivision = new byte[size];
		mStartIndex = new short[size];
	}

	/**
	 * Set the subdivision for the indicated cell. By default all cells have no sub-divisions (0). A sub-division of 1,
	 * means the cell is split into 4 cells. A sub-division of 2, means it is split into 16, etc.
	 * 
	 * @param row
	 * @param col
	 * @param subdivision
	 */
	public void setSubdivision(int row, int col, byte subdivision)
	{
		int rc = posSD(row, col);
		if (rc >= 0 && rc < mSubdivision.length)
		{
			mSubdivision[rc] = subdivision;
			/*
			 * There are no set-able subdivisions beyond the edge.
			 * However, for the sake of computation we pretend there is.
			 * We set it to be the same as the edge.
			 */
			if (row == mNumRows - 1)
			{
				mSubdivision[posSD(row + 1, col)] = subdivision;
			}
			if (col == mNumCols - 1)
			{
				mSubdivision[posSD(row, col + 1)] = subdivision;
			}
		}
	}
}
