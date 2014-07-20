/**
 * Copyright 2014, TIP Solutions, Inc. All rights reserved.
 */
package com.tipsolutions.panda.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GlobalInt
{
	static final String			KEY_NAME					= DatabaseManager.KEY_NAME;
	static final String			KEY_ROWID					= DatabaseManager.KEY_ROWID;
	static final String			KEY_VALUE					= DatabaseManager.KEY_VALUE;

	static final String			NAME_RATIO					= "ratio";

	public static final String	TABLE_GLOBALS_INT			= "globals_int";
	static final String			TABLE_GLOBALS_INT_CREATE	= "create table " + TABLE_GLOBALS_INT + " (" + KEY_ROWID
																	+ " integer primary key autoincrement, " + KEY_NAME
																	+ " text, " + KEY_VALUE + " integer);";

	public static void create(SQLiteDatabase db) throws SQLException
	{
		db.execSQL(TABLE_GLOBALS_INT_CREATE);
	}

	public static void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_GLOBALS_INT, null, null);
	}

	static Long query(SQLiteDatabase db, String key, Long defaultValue)
	{
		Long value = defaultValue;
		try
		{
			Cursor cursor = db.query(TABLE_GLOBALS_INT, new String[] {
				KEY_VALUE }, KEY_NAME + "=?", new String[] {
				key }, null, null, null);
			if (cursor.moveToFirst())
			{
				value = cursor.getLong(0);
			}
			cursor.close();
		}
		catch (Exception ex)
		{
			Log.e(DatabaseManager.TAG, ex.getMessage());
		}
		return value;
	}

	static void set(SQLiteDatabase db, String key, long value)
	{
		db.beginTransaction();
		try
		{
			ContentValues values = new ContentValues();
			values.put(KEY_VALUE, value);
			if (db.update(TABLE_GLOBALS_INT, values, KEY_NAME + "=?", new String[] {
				key }) == 0)
			{
				values.put(KEY_NAME, key);
				db.insert(TABLE_GLOBALS_INT, null, values);
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
