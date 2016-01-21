/**
 * Copyright 2014, TIP Solutions, Inc. All rights reserved.
 */
package com.tipsolutions.panda.data;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.util.Log;

public class Strokes
{
	class Stroke
	{
		long	mId	= -1;
		short	mStartX;
		short	mStartY;
		short	mStrokeNum;
		short	mWidth;
		byte[]	mData;
		CellMap	mMap;

		Stroke(long id, short stroke, short startX, short startY, short width, byte[] data)
		{
			mId = id;
			mStrokeNum = stroke;
			mStartX = startX;
			mStartY = startY;
			mWidth = width;
			mData = data;
		}

		public CellMap getCellMap() throws Exception
		{
			if (mMap == null)
			{
				mMap = new CellMap(mData, mWidth);
			}
			return mMap;
		}

		public int getEndX()
		{
			return mStartX + mWidth;
		}

		public int getEndY() throws Exception
		{
			return mStartY + getHeight();
		}

		public int getHeight() throws Exception
		{
			int height = (mData.length * 8) / mWidth;

			int expectedsize = mWidth * height;

			if (expectedsize != mData.length * 8)
			{
				throw new Exception("Expected data size of " + expectedsize + ", found " + mData.length * 8
						+ ". Data length=" + mData.length + ", Data width=" + mWidth + ", Num Rows=" + height);
			}
			return height;
		}

		public int getStartX()
		{
			return mStartX;
		}

		public int getStartY()
		{
			return mStartY;
		}

		public int getStrokeNum()
		{
			return mStrokeNum;
		}

		public int getWidth()
		{
			return mWidth;
		}

		void update(SQLiteDatabase db)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_ELEMENT_ID, mElementId);
			values.put(KEY_STROKE_NUM, mStrokeNum);
			values.put(KEY_STARTX, mStartX);
			values.put(KEY_STARTY, mStartY);
			values.put(KEY_DATA, mData);

			if (mId < 0 || db.update(TABLE_STROKES, values, KEY_ROWID + "=?", new String[] {
				Long.toString(mId) }) == 0)
			{
				mId = db.insert(TABLE_STROKES, null, values);
			}
		}
	}

	static final int	MAX_NUM_CELL_X			= 100;
	static final int	MAX_NUM_CELL_Y			= 100;
	static final int	MAX_CELLS				= MAX_NUM_CELL_X * MAX_NUM_CELL_Y;
	static final int	MAX_CELLS_BYTES			= MAX_CELLS / 8 + 1;

	static final String	KEY_ROWID				= DatabaseManager.KEY_ROWID;
	static final String	KEY_ELEMENT_ID			= "ele_id";
	static final String	KEY_STROKE_NUM			= "stroke_num";
	static final String	KEY_STARTX				= "startx";
	static final String	KEY_STARTY				= "starty";
	static final String	KEY_WIDTH				= "width";
	static final String	KEY_DATA				= "data";

	static final String	TABLE_STROKES			= "strokes";
	static final String	TABLE_STROKES_CREATE	= "create table " + TABLE_STROKES + " (" + KEY_ROWID
														+ " integer primary key autoincrement, " + KEY_ELEMENT_ID
														+ " integer, " + KEY_STROKE_NUM + " tinyint, " + KEY_STARTX
														+ " smallint, " + KEY_STARTY + " smallint, " + KEY_WIDTH
														+ " smallint, " + KEY_DATA + " varbinary[" + MAX_CELLS_BYTES
														+ "]);";

	public static void create(SQLiteDatabase db) throws SQLException
	{
		db.execSQL(TABLE_STROKES_CREATE);
	}

	public static void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_STROKES, null, null);
	}

	public static Strokes query(SQLiteDatabase db, int eleId) throws Exception
	{
		String where = KEY_ELEMENT_ID + "=?";
		String[] whereArgs = new String[] {
			Long.toString(eleId) };
		StringBuilder orderby = new StringBuilder(50);
		orderby.append(KEY_STROKE_NUM).append(" ASC");

		Cursor cursor = db.query(TABLE_STROKES, null, where, whereArgs, null, null, orderby.toString());

		int idxRowId = cursor.getColumnIndex(KEY_ROWID);
		int idxStrokeNum = cursor.getColumnIndex(KEY_STROKE_NUM);
		int idxStartX = cursor.getColumnIndex(KEY_STARTX);
		int idxStartY = cursor.getColumnIndex(KEY_STARTY);
		int idxWidth = cursor.getColumnIndex(KEY_WIDTH);
		int idxData = cursor.getColumnIndex(KEY_DATA);
		long rowId;
		short strokeId;
		short startX;
		short startY;
		short width;
		byte[] data;
		Strokes strokes = new Strokes(eleId);
		Stroke stroke;

		while (cursor.moveToNext())
		{
			rowId = cursor.getLong(idxRowId);
			strokeId = cursor.getShort(idxStrokeNum);
			startX = cursor.getShort(idxStartX);
			startY = cursor.getShort(idxStartY);
			width = cursor.getShort(idxWidth);
			data = cursor.getBlob(idxData);
			stroke = strokes.new Stroke(rowId, strokeId, startX, startY, width, data);
			strokes.add(stroke);
		}
		cursor.close();
		return strokes;
	}

	int					mElementId;
	Rect				mBounds	= null;
	ArrayList<Stroke>	mStrokes;
	CellMap				mMap;

	Strokes(int eleId)
	{
		mElementId = eleId;
		mStrokes = new ArrayList<Stroke>();
	}

	public void add(Stroke stroke) throws Exception
	{
		mStrokes.add(stroke);

		if (mBounds == null)
		{
			mBounds = new Rect(stroke.getStartX(), stroke.getStartY(), stroke.getEndX(), stroke.getEndY());
		}
		else
		{
			int val;

			if (stroke.getStartX() < mBounds.left)
			{
				mBounds.left = stroke.getStartX();
			}
			if (stroke.getEndX() > mBounds.right)
			{
				mBounds.right = stroke.getEndX();
			}
			if (stroke.getStartY() < mBounds.top)
			{
				mBounds.top = stroke.getStartY();
			}
			if ((val = stroke.getEndY()) > mBounds.right)
			{
				mBounds.right = val;
			}
		}
	}

	public Rect getBounds()
	{
		return mBounds;
	}

	public CellMap getCellMap() throws Exception
	{
		if (mMap == null)
		{
			mMap = new CellMap(mBounds.width(), mBounds.height());

			for (Stroke stroke : mStrokes)
			{
				mMap.overlap(stroke.getCellMap(), stroke.getStartX(), stroke.getStartY());
			}
		}
		return mMap;
	}

	public int getId()
	{
		return mElementId;
	}

	public ArrayList<Stroke> getStrokes()
	{
		return mStrokes;
	}

	public void update(SQLiteDatabase db)
	{
		db.beginTransaction();
		try
		{
			for (Stroke stroke : mStrokes)
			{
				stroke.update(db);
			}
			db.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			Log.e(DatabaseManager.TAG, ex.getMessage());
		}
		finally
		{
			db.endTransaction();
		}
	}

}
