/**
 * Copyright 2014, TIP Solutions, Inc. All rights reserved.
 */
package com.tipsolutions.panda.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Strokes
{
	class Stroke
	{
		int		mCellX;
		int		mCellY;
		short	mStroke;
		long	mId	= -1;

		Stroke(long id, short stroke, int cellX, int cellY)
		{
			mId = id;
			mStroke = stroke;
			mCellX = cellX;
			mCellY = cellY;
		}

		public int getCellX()
		{
			return mCellX;
		}

		public int getCellY()
		{
			return mCellY;
		}

		void update(SQLiteDatabase db)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_ELEMENT_ID, mElementId);
			values.put(KEY_STROKE_ID, mStroke);
			values.put(KEY_CELLX, mCellX);
			values.put(KEY_CELLY, mCellY);
			if (mId < 0 || db.update(TABLE_STROKES, values, KEY_ROWID + "=?", new String[] {
				Long.toString(mId) }) == 0)
			{
				mId = db.insert(TABLE_STROKES, null, values);
			}
		}
	}

	static final int	NUM_CELL_X				= 100;
	static final int	NUM_CELL_Y				= 100;

	static final String	KEY_ROWID				= DatabaseManager.KEY_ROWID;
	static final String	KEY_ELEMENT_ID			= "ele_id";
	static final String	KEY_STROKE_ID			= "stroke";
	static final String	KEY_CELLX				= "x";
	static final String	KEY_CELLY				= "y";

	static final String	TABLE_STROKES			= "strokes";
	static final String	TABLE_STROKES_CREATE	= "create table " + TABLE_STROKES + " (" + KEY_ROWID
														+ " integer primary key autoincrement, " + KEY_ELEMENT_ID
														+ " integer, " + KEY_STROKE_ID + " tinyint, " + KEY_CELLX
														+ " int, " + KEY_CELLY + " int);";

	public static void create(SQLiteDatabase db) throws SQLException
	{
		db.execSQL(TABLE_STROKES_CREATE);
	}

	public static void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_STROKES, null, null);
	}

	public static ArrayList<Strokes> query(SQLiteDatabase db)
	{
		ArrayList<Strokes> list = new ArrayList<Strokes>();
		StringBuilder orderby = new StringBuilder(50);
		orderby.append(KEY_ELEMENT_ID).append(", ").append(KEY_STROKE_ID).append(" ASC");

		Cursor cursor = db.query(TABLE_STROKES, null, null, null, null, null, orderby.toString());

		int idxRowId = cursor.getColumnIndex(KEY_ROWID);
		int idxElementId = cursor.getColumnIndex(KEY_ELEMENT_ID);
		int idxStrokeId = cursor.getColumnIndex(KEY_STROKE_ID);
		int idxCellX = cursor.getColumnIndex(KEY_CELLX);
		int idxCellY = cursor.getColumnIndex(KEY_CELLY);
		int eleId;
		int curEleId = -1;
		long rowId;
		short strokeId;
		int cellX;
		int cellY;
		Strokes strokes = null;
		Stroke stroke;

		while (cursor.moveToNext())
		{
			eleId = cursor.getInt(idxElementId);
			if (eleId != curEleId)
			{
				strokes = new Strokes(eleId);
				curEleId = eleId;
			}
			rowId = cursor.getLong(idxRowId);
			strokeId = cursor.getShort(idxStrokeId);
			cellX = cursor.getInt(idxCellX);
			cellY = cursor.getInt(idxCellY);
			stroke = strokes.new Stroke(rowId, strokeId, cellX, cellY);
			strokes.mStrokes.add(stroke);
		}
		cursor.close();
		return list;
	}

	public static Strokes query(SQLiteDatabase db, int eleId)
	{
		String where = KEY_ELEMENT_ID + "=?";
		String[] whereArgs = new String[] {
			Long.toString(eleId) };

		Cursor cursor = db.query(TABLE_STROKES, null, where, whereArgs, null, null, null);

		int idxRowId = cursor.getColumnIndex(KEY_ROWID);
		int idxStrokeId = cursor.getColumnIndex(KEY_STROKE_ID);
		int idxCellX = cursor.getColumnIndex(KEY_CELLX);
		int idxCellY = cursor.getColumnIndex(KEY_CELLY);
		long rowId;
		short strokeId;
		short cellX;
		short cellY;
		Strokes strokes = new Strokes(eleId);
		Stroke stroke;

		while (cursor.moveToNext())
		{
			rowId = cursor.getLong(idxRowId);
			strokeId = cursor.getShort(idxStrokeId);
			cellX = cursor.getShort(idxCellX);
			cellY = cursor.getShort(idxCellY);
			stroke = strokes.new Stroke(rowId, strokeId, cellX, cellY);
			strokes.mStrokes.add(stroke);
		}
		cursor.close();
		return strokes;
	}

	int					mElementId;
	ArrayList<Stroke>	mStrokes;

	Strokes(int eleId)
	{
		mElementId = eleId;
		mStrokes = new ArrayList<Stroke>();
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
