package com.dugsolutions.nerdypig.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dugsolutions.nerdypig.MyApplication;

/**
 * Created by dug on 12/18/16.
 */

public class DatabaseManager
{
	public static void Init(MyApplication app)
	{
		if (sInstance == null)
		{
			sInstance = new DatabaseManager(app);
			sInstance.open();
		}
	}

	static DatabaseManager sInstance;

	public static DatabaseManager getInstance()
	{
		return sInstance;
	}

	public static SQLiteDatabase getDB()
	{
		return getInstance().mDb;
	}

	static final String			TAG					= "DB";

	static final String			DATABASE_NAME		= "pig.db";
	static final int			DATABASE_VERSION	= 1;

	public static final String	KEY_NAME			= "name";
	public static final String	KEY_ROWID			= "_id";
	public static final String	KEY_VALUE			= "value";

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
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

			if (oldVersion < DATABASE_VERSION)
			{
			}
		}
	}

	MyApplication	mApp;
	SQLiteDatabase	mDb;
	DatabaseHelper	mDbHelper;

	public DatabaseManager(MyApplication app)
	{
		mApp = app;
	}

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

	public void close()
	{
		if (mDbHelper != null)
		{
			mDbHelper.close();
			mDbHelper = null;
		}
	}

	void resetDatabase()
	{
		GlobalInt.deleteAll();
	}

}
