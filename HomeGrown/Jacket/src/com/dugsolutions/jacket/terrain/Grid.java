package com.dugsolutions.jacket.terrain;

import java.util.HashMap;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.BufUtils.FloatBuf;
import com.dugsolutions.jacket.math.BufUtils.ShortBuf;
import com.dugsolutions.jacket.math.BufUtils.TmpFloatBuf;
import com.dugsolutions.jacket.math.BufUtils.TmpShortBuf;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.MathUtils;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * Height map which supports an arbitrarily complex geometry using generators.
 * <p>
 * Generates vertex, normal and tex arrays for all points in the grid.
 * <p>
 * Index generation uses full triangles.
 */
public class Grid implements IMapData
{
	public class PointMap
	{
		PointInfo[]	mPoints;
		int			mNumRows;
		int			mNumCols;

		PointMap(int numRows, int numCols)
		{
			mNumCols = numCols;
			mNumRows = numRows;
			mPoints = new PointInfo[mNumRows * mNumCols];
		}

		PointInfo get(int row, int col)
		{
			return mPoints[getIndex(row, col)];
		}

		void put(int row, int col, PointInfo info)
		{
			mPoints[getIndex(row, col)] = info;
		}

		int getIndex(int row, int col)
		{
			return row * mNumCols + col;
		}
	}

	class BufferResult
	{
		PointMap	mPointMap;
		TmpFloatBuf	mVertexBuf;
		TmpFloatBuf	mNormalBuf;
		TmpFloatBuf	mColorBuf;
		TmpFloatBuf	mTexBuf;
		TmpShortBuf	mIndexBuf;
		short		mPosition;

		public BufferResult(int numRows, int numCols)
		{
			this();

			if (mUsePointMap)
			{
				mPointMap = new PointMap(numRows, numCols);
			}
		}

		public BufferResult()
		{
			mVertexBuf = new TmpFloatBuf();
			mTexBuf = new TmpFloatBuf();
			mIndexBuf = new TmpShortBuf();

			if (mWithNormals)
			{
				mNormalBuf = new TmpFloatBuf();
				mNormalDefault = new Vector3f(0, 0, 1);
			}
			else
			{
				mNormalDefault = null;
				mColorBuf = new TmpFloatBuf();
			}
		}

		public void calcColors()
		{
			if (mColorBuf != null)
			{
				mColorBuf.clear();
				Color4f color;
				for (int p = 0; p + 2 < mVertexBuf.size(); p += 3)
				{
					color = mComputeColor.getColor(mVertexBuf.get(p), mVertexBuf.get(p + 1), mVertexBuf.get(p + 2));
					mColorBuf.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
				}
			}
		}

		public void cleanup()
		{
			mVertexBuf.clear();
			mTexBuf.clear();
			mIndexBuf.clear();

			if (mNormalBuf != null)
			{
				mNormalBuf.clear();
			}
			if (mColorBuf != null)
			{
				mColorBuf.clear();
			}
		}

		public FloatBuf getColorBuf()
		{
			if (mColorBuf != null && mColorBuf.size() > 0)
			{
				return mColorBuf.create();
			}
			return null;
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
			if (mNormalBuf != null && mNormalBuf.size() > 0)
			{
				return mNormalBuf.create();
			}
			return null;
		}

		int getNumVecs()
		{
			return mVertexBuf.size() / 3;
		}

		public FloatBuf getTexBuf()
		{
			return mTexBuf.create();
		}

		public FloatBuf getVertexBuf()
		{
			return mVertexBuf.create();
		}

		public void put(int row, int col, float x, float y, float percentX, float percentY)
		{
			Vector3f normal;
			Vector3f vec;
			PointInfo ptInfo;

			if (mPointMap != null)
			{
				ptInfo = mPointMap.get(row, col);
			}
			else
			{
				ptInfo = null;
			}
			if (ptInfo == null)
			{
				Info info = new Info();
				info.setGenNormal(mWithNormals);

				mCompute.fillInfo(x, y, info);

				vec = new Vector3f(x + info.getXAdjust(), y + info.getYAdjust(), info.getHeight());

				if (mWithNormals)
				{
					normal = info.getNormal();
					if (normal == null)
					{
						normal = mNormalDefault;
					}
				}
				else
				{
					normal = null;
				}
				ptInfo = new PointInfo(vec, normal);

				if (mPointMap != null)
				{
					mPointMap.put(row, col, ptInfo);
				}
			}
			else
			{
				vec = ptInfo.getVec();
				normal = ptInfo.getNormal();
			}
			ptInfo.addPosition(mVertexBuf.size());
			mVertexBuf.put(vec.getX()).put(vec.getY()).put(vec.getZ());

			if (mNormalBuf != null && normal != null)
			{
				mNormalBuf.put(normal.getX()).put(normal.getY()).put(normal.getZ());
			}
			mTexBuf.put(percentX).put(percentY);
			mPosition++;
		}

		public void putZ(PointInfo info)
		{
			for (int pos : info.getPositions())
			{
				mVertexBuf.put(pos + 2, info.getVec().getZ());
			}
		}

		public void put(int id, HashMap<Integer, Short> map, int row, int col, float x, float y, float percentX,
				float percentY)
		{
			map.put(id, getNextPosition());
			put(row, col, x, y, percentX, percentY);
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

	static final String		TAG				= "Grid";

	int						mNumRows;
	int						mNumCols;
	int						mTimesRow		= 1;
	int						mTimesCol		= 1;
	float					mIncX;
	float					mIncY;
	/** Track index by row,col,subrow,subcol for vertex, normal, and tex arrays */
	HashMap<Integer, Short>	mIndexTL		= new HashMap<Integer, Short>();
	/** On repeating texture maps, need secondary points along edge boundaries */
	/** Top-Right, Column Edge repeats */
	HashMap<Integer, Short>	mIndexTR		= new HashMap<Integer, Short>();
	/** Bottom-Left, Row Edge repeats */
	HashMap<Integer, Short>	mIndexBL		= new HashMap<Integer, Short>();
	/** Bottom-Right Row,Col Edge repeats */
	HashMap<Integer, Short>	mIndexBR		= new HashMap<Integer, Short>();

	/** Shared result */
	BufferResult			mResult;
	/** Computational bounds of grid */
	Bounds2D				mBounds;
	/** Used to compute the actual values */
	ICalcValue				mCompute;
	/** Special calculation to handle applying color shades across different heights */
	ICalcColor				mComputeColor;
	Vector3f				mNormalDefault;
	/** If used, then we are computing normal values too */
	boolean					mWithNormals	= true;
	boolean					mUsePointMap;

	/**
	 * 
	 * @param nrows
	 * @param ncols
	 */
	public Grid(int nrows, int ncols)
	{
		setSize(nrows, ncols);
	}

	public void calc(ICalcValue calc)
	{
		mCompute = calc;
		mResult = new BufferResult(mNumRows + 1, mNumCols + 1);
		calcPoints();
		mCompute.postCalc(this);

		if (mComputeColor != null)
		{
			mResult.calcColors();
		}
		calcIndexes();
	}

	/**
	 * Calculate all the triangles by filling up the index array.
	 */
	void calcIndexes()
	{
		short indexes[] = new short[4]; // 0: this, 1: right, 2:bottom, 3: bottom-right

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
		float y = mBounds.getMaxY();
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

		mIncX = incX;
		mIncY = incY;
		/*
		 * Do each cell. There is one point per cell.
		 * 
		 * Note: the order in which the points are added are important. The index array created later refers to this
		 * order.
		 * 
		 * The right & bottom edge are treated as if it were yet another cell in terms of where the index array value
		 * is, but we only have a single row of points. The subdivision value for these cells share the neighbor edge
		 * cell.
		 * 
		 * A complication: on the edge of the texture we need to double up the points depending on which side
		 * of the texture needs to be represented. Upper triangles use the lower portion of the texture on the edge,
		 * Lower triangles use the upper portion of the texture on the edge. Even though both points are physically in
		 * the same spot.
		 */
		mIndexTL.clear();
		mIndexTR.clear();
		mIndexBL.clear();
		mIndexBR.clear();

		if (!isRepeating())
		{
			float percentIncX = 1f / mNumCols;
			float percentIncY = 1f / mNumRows;

			for (int row = 0; row <= mNumRows; row++)
			{
				x = mBounds.getMinX();
				percentX = 0;

				for (int col = 0; col <= mNumCols; col++)
				{
					mResult.put(row, col, x, y, percentX, percentY);
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
				x = mBounds.getMinX();

				percentXCounter = 0;
				percentX = 0;
				percentX2 = 0;

				for (int col = 0; col <= mNumCols; col++)
				{
					id = ID(row, col);

					mResult.put(id, mIndexTL, row, col, x, y, percentX, percentY);

					if (percentX != percentX2)
					{
						if (percentY != percentY2)
						{
							mResult.put(id, mIndexTR, row, col, x, y, percentX2, percentY);
							mResult.put(id, mIndexBR, row, col, x, y, percentX2, percentY2);
							mResult.put(id, mIndexBL, row, col, x, y, percentX, percentY2);
						}
						else
						{
							mResult.put(id, mIndexTR, row, col, x, y, percentX2, percentY);
						}
					}
					else if (percentY2 != percentY)
					{
						mResult.put(id, mIndexBL, row, col, x, y, percentX, percentY2);
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

	public FloatBuf getCalcColorBuf()
	{
		return mResult.getColorBuf();
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

	@Override
	public int[] getBoundary(Bounds2D bounds)
	{
		if (mResult.mPointMap == null)
		{
			return null;
		}
		Vector3f vec;
		int startRow = -1;
		int startCol = -1;
		int endRow = mNumRows;
		int endCol = mNumCols;

		for (int row = 0; row < mNumRows; row++)
		{
			vec = mResult.mPointMap.get(row, 0).getVec();

			if (startRow == -1)
			{
				if (vec.getY() >= bounds.getMinY() && vec.getY() <= bounds.getMaxY())
				{
					startRow = row;

					for (int col = 0; col < mNumCols; col++)
					{
						vec = mResult.mPointMap.get(row, col).getVec();

						if (startCol == -1)
						{
							if (vec.getX() >= bounds.getMinX() && vec.getX() <= bounds.getMaxX())
							{
								startCol = col;
							}
						}
						else if (!(vec.getX() >= bounds.getMinX() && vec.getX() <= bounds.getMaxX()))
						{
							endCol = col - 1;
							break;
						}
					}
				}
			}
			else if (!(vec.getY() >= bounds.getMinY() && vec.getY() <= bounds.getMaxY()))
			{
				endRow = row - 1;
				break;
			}
		}
		if ((startRow == -1) || (startCol == -1))
		{
			return null;
		}
		return new int[] {
				startRow, startCol, endRow, endCol };
	}

	public float getHeight()
	{
		return mBounds.getSizeY();
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

	@Override
	public PointInfo getPointInfo(int row, int col)
	{
		if (mResult.mPointMap == null)
		{
			return null;
		}
		return mResult.mPointMap.get(row, col);
	}

	public float getWidth()
	{
		return mBounds.getSizeX();
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

	@Override
	public void putZ(PointInfo info)
	{
		mResult.putZ(info);
	}

	public Grid setBounds(Bounds2D bounds)
	{
		mBounds = bounds;
		return this;
	}

	public Grid setComputeColor(ICalcColor calc)
	{
		mComputeColor = calc;
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
	 * @param ncols
	 * @throws Exception
	 */
	public void setSize(int nrows, int ncols)
	{
		mNumRows = nrows;
		mNumCols = ncols;
	}

	public Grid setUsePointMap(boolean flag)
	{
		mUsePointMap = flag;
		return this;
	}

	public void setWithNormals(boolean withNormal)
	{
		mWithNormals = withNormal;
	}
}
