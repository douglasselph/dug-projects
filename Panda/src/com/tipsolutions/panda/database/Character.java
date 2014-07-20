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

public class Character
{
	static final String	KEY_ROWID				= DatabaseManager.KEY_ROWID;
	static final String	KEY_NAME				= "name";
	static final String	KEY_STROKES_ID			= "strokes_id";
	static final String	KEY_PINGYING			= "pingying";

	static final String	TABLE_CHARACTER			= "character";
	static final String	TABLE_CHARACTER_CREATE	= "create table " + TABLE_CHARACTER + " (" + KEY_ROWID
														+ " integer primary key autoincrement, " + KEY_NAME
														+ " string, " + KEY_STROKES_ID + " int default 0, "
														+ KEY_PINGYING + " string);";

	public static void create(SQLiteDatabase db) throws SQLException
	{
		db.execSQL(TABLE_CHARACTER_CREATE);
	}

	public static void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_CHARACTER, null, null);
	}

	public static ArrayList<Character> query(SQLiteDatabase db)
	{
		StringBuilder orderby = new StringBuilder(50);
		orderby.append(KEY_NAME).append(" ASC");

		Cursor cursor = db.query(TABLE_CHARACTER, null, null, null, null, null, orderby.toString());

		return query(db, cursor);
	}

	static ArrayList<Character> query(SQLiteDatabase db, Cursor cursor)
	{
		ArrayList<Character> list = new ArrayList<Character>();

		int idxRowId = cursor.getColumnIndex(KEY_ROWID);
		int idxNameId = cursor.getColumnIndex(KEY_NAME);
		int idxStrokesId = cursor.getColumnIndex(KEY_STROKES_ID);
		int idxPingYing = cursor.getColumnIndex(KEY_PINGYING);
		int strokesId;
		Character character;

		while (cursor.moveToNext())
		{
			character = new Character();
			character.mRowId = cursor.getLong(idxRowId);
			character.mName = cursor.getString(idxNameId);
			character.mPingYing = cursor.getString(idxPingYing);
			strokesId = cursor.getInt(idxStrokesId);
			character.mStrokes = Strokes.query(db, strokesId);
			list.add(character);
		}
		cursor.close();
		return list;
	}

	public static ArrayList<Character> query(SQLiteDatabase db, String name)
	{
		String where = KEY_NAME + "=?";
		String[] whereArgs = new String[] {
			name };
		Cursor cursor = db.query(TABLE_CHARACTER, null, where, whereArgs, null, null, null);
		return query(db, cursor);
	}

	long	mRowId	= -1;
	String	mName;
	String	mPingYing;
	Strokes	mStrokes;

	Character()
	{
	}

	public void update(SQLiteDatabase db)
	{
		db.beginTransaction();
		try
		{
			ContentValues values = new ContentValues();
			values.put(KEY_NAME, mName);
			values.put(KEY_PINGYING, mPingYing);
			if (mStrokes != null)
			{
				values.put(KEY_STROKES_ID, mStrokes.getId());
			}
			if (mRowId < 0 || db.update(TABLE_CHARACTER, values, KEY_ROWID + "=?", new String[] {
				Long.toString(mRowId) }) == 0)
			{
				mRowId = db.insert(TABLE_CHARACTER, null, values);
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
