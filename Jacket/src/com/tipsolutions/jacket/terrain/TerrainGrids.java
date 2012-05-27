package com.tipsolutions.jacket.terrain;

import java.util.ArrayList;

public class TerrainGrids {

	ArrayList<TerrainGrid> mGrids = new ArrayList<TerrainGrid>();
	float mHeight = 1;
	int mNumCols = 10;
	int mNumRows = 10;
	float mStartX = 0;
	float mStartY = 0;
	float mWidth = 1;
	
	public TerrainGrids() {
	}
	
	public TerrainGrids addGrid(TerrainGrid grid) {
		mGrids.add(grid);
		return this;
	}
	
	public TerrainGrid getGrid(int i) {
		return mGrids.get(i);
	}
	
	public ArrayList<TerrainGrid> getGrids() {
		return mGrids;
	}
	
	public float getHeight() {
		return mHeight;
	}
	
	public int getNumCols() {
		return mNumCols;
	}
	
	public int getNumGrids() {
		return mGrids.size();
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
	
	public TerrainGrids setDimension(float width, float height) {
		mWidth = width;
		mHeight = height;
		return this;
	}
	
	public TerrainGrids setGranularity(int nrows, int ncols) {
		mNumRows = (int) (mHeight * nrows);
		mNumCols = (int) (mWidth * ncols);
		return this;
	}
	
	public TerrainGrids setGridSize(int nrows, int ncols) {
		mNumRows = nrows;
		mNumCols = ncols;
		return this;
	}

	public TerrainGrids setStartXY(float x, float y) {
		mStartX = x;
		mStartY = y;
		return this;
	}
}
