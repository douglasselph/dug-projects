package com.dugsolutions.felldungeon;

public class FellMap {

	// Each cell is made up of a number of units.
	class Unit
	{
		short elevation;
	}
	
	// One cell or unit on the map.
	class Cell
	{
		float [] mVertices; // Each cell has a separate mesh to display itself.
		short mTerrain; // The terrain that lives here.
		Unit [] mUnits;
		
		Cell()
		{
			mUnits = new Unit[mTextureSize * mTextureSize];
		}
	}
	
	
	static final int mNumVerticesPerUnit = 5;
	
	int mWidth;
	int mHeight;
	int mTextureSize; // how many units per cell.
	Cell [] mCells;
	float [] mVertices;
	boolean mVerticesStale;
	
	public FellMap(int width, int height, int textureSize)
	{
		mTextureSize = textureSize;
		setSize(width, height);
	}
	
	public void setSize(int width, int height)
	{
		mWidth = width;
		mHeight = height;
		mVerticesStale = true;
	}
	
	void buildVertices()
	{
		mVertices = new float[mWidth * mHeight * 6 * mNumVerticesPerUnit];
		mVerticesStale = false;
	}
	
	public float [] getVertices()
	{
		if (mVerticesStale || mVertices == null)
		{
			buildVertices();
		}
		return mVertices;
	}
}
