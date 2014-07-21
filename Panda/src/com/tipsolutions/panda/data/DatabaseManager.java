/**
 * Copyright 2014, TIP Solutions, Inc. All rights reserved.
 */
package com.tipsolutions.panda.data;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tipsolutions.panda.MyApplication;

/**
 * Simple database access helper class. Defines the basic CRUD operations.
 */
public class DatabaseManager
{
	static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context ctx)
		{
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			try
			{
				Strokes.create(db);
				Character.create(db);
				GlobalInt.create(db);
			}
			catch (Exception ex)
			{
				Log.e(TAG, ex.getMessage());
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
		}
	}

	static final boolean		LOG					= MyApplication.LOG;
	static final String			TAG					= MyApplication.TAG + ".DB";
	static final String			DATABASE_NAME		= "panda.db";
	static final int			DATABASE_VERSION	= 1;

	public static final String	KEY_NAME			= "name";
	public static final String	KEY_ROWID			= "_id";
	public static final String	KEY_VALUE			= "value";

	final MyApplication			mApp;
	SQLiteDatabase				mDb;
	DatabaseHelper				mDbHelper;

	public DatabaseManager(MyApplication ctx)
	{
		mApp = ctx;
	}

	/**
	 * Close the database.
	 */
	public void close()
	{
		if (mDbHelper != null)
		{
			mDbHelper.close();
			mDbHelper = null;
		}
	}

	/**
	 * Accessor for database.
	 * 
	 * @return
	 */
	public SQLiteDatabase getDB()
	{
		return mDb;
	}

	public long getInteger(String key, long defValue)
	{
		return GlobalInt.query(mDb, key, defValue);
	}

	/**
	 * Open the database. If it cannot be opened, try to create a new instance
	 * of the database. If it cannot be created, throw an exception to signal
	 * the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *         if the database could be neither opened or created
	 */
	public DatabaseManager open() throws SQLException
	{
		if (mDbHelper == null || mDb == null || !mDb.isOpen())
		{
			mDbHelper = new DatabaseHelper(mApp);
			mDb = mDbHelper.getWritableDatabase();
			if (!mDb.isOpen())
			{
				resetDatabase();
			}
		}
		return this;
	}

	public void resetDatabase()
	{
		Strokes.deleteAll(mDb);
		GlobalInt.deleteAll(mDb);
		Character.deleteAll(mDb);
	}

	public void setInteger(String key, long value)
	{
		GlobalInt.set(mDb, key, value);
	}

}
