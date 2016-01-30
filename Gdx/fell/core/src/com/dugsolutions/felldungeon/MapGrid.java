package com.dugsolutions.felldungeon;

public class MapGrid {

	// Holds a grid of MapSquares.
	
	// Each Square is made potentially made up of several different elevations.
	class Unit
	{
		short elevation;
	}
	
	// Each square on the map holds one texture region from the atlas.
	class Square
	{
		float [] mVertices; // Each cell has a separate mesh to display itself.
		short mTerrain; // The terrain that lives here.
		Unit [] mUnits;

	}
	
//	
//	static final int mNumVerticesPerUnit = 5;
//	
//	int mWidth;
//	int mHeight;
//	int mTextureSize; // how many units per cell.
//	Cell [] mCells;
//	float [] mVertices;
//	boolean mVerticesStale;
//	
//	public MapGrid(int width, int height, int textureSize)
//	{
//		mTextureSize = textureSize;
//		setSize(width, height);
//	}
//	
//	public void setSize(int width, int height)
//	{
//		mWidth = width;
//		mHeight = height;
//		mVerticesStale = true;
//	}
//	
//	void buildVertices()
//	{
//		mVertices = new float[mWidth * mHeight * 6 * mNumVerticesPerUnit];
//		mVerticesStale = false;
//	}
//	
//	public float [] getVertices()
//	{
//		if (mVerticesStale || mVertices == null)
//		{
//			buildVertices();
//		}
//		return mVertices;
//	}
}
