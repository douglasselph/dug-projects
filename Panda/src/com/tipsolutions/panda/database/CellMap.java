package com.tipsolutions.panda.database;

import java.util.ArrayList;
import java.util.Arrays;

import com.tipsolutions.panda.database.Strokes.Stroke;

public class CellMap
{
	int		mNumX;
	int		mNumY;
	byte[]	mMap;

	public CellMap(int numx, int numy)
	{
		mNumX = numx;
		mNumY = numy;
		int size = mNumX * mNumY;
		int size8 = size / 8 + 1;
		mMap = new byte[size8];
	}

	void clear()
	{
		byte val = 0;
		Arrays.fill(mMap, val);
	}

	void clear(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		mMap[byteP] &= ~val;
	}

	public boolean is(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		return (mMap[byteP] & val) == val;
	}

	void set(int x, int y)
	{
		int pos = y * mNumX + x;
		int byteP = pos / 8;
		int byteR = pos % 8;
		int val = 1 << byteR;
		mMap[byteP] |= val;
	}

	void fill(Strokes strokes)
	{
		clear();

		int startX;
		int startY;
		int endX;
		int endY;
		ArrayList<Stroke> list = strokes.getStrokes();
		Stroke stroke;

		for (int i = 0; i + 1 < list.size(); i++)
		{
			stroke = list.get(i++);
			startX = stroke.getCellX();
			startY = stroke.getCellY();
			stroke = list.get(i);
			endX = stroke.getCellX();
			endY = stroke.getCellY();
		}
	}
}
