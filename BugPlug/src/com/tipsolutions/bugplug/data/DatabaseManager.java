package com.tipsolutions.bugplug.data;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tipsolutions.bugplug.MyApplication;

public class DatabaseManager {

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		    try {
    			db.execSQL(TABLE_MAP_CREATE);
    			db.execSQL(TABLE_TERRAIN_CREATE);
		    } catch (Exception ex) {
		    	Log.e(TAG, ex.getMessage());
		    }
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
		}
	}
	static final String TAG = MyApplication.TAG;
	
	static final Boolean LOG = true;
	static final int DATABASE_VERSION = 1;

	static final String DATABASE_NAME = "bugplug.db";
	static final String TABLE_MAP = "map";

	static final String TABLE_TERRAIN = "terrain";
	static final String KEY_ROWID = "_id";
	static final String KEY_X = "x";
	static final String KEY_Y = "y";
	static final String KEY_TERRAIN_ID = "terrain_id";

	static final String KEY_FILENAME = "filename";

	static final String TABLE_MAP_CREATE = "create table "
			+ TABLE_MAP + " (" 
			+ KEY_ROWID + " integer primary key autoincrement, " 
			+ KEY_X + " smallint not null, " 
			+ KEY_Y + " smallint not null, "
			+ KEY_TERRAIN_ID + " integer)";
	
	static final String TABLE_TERRAIN_CREATE = "create table "
			+ TABLE_TERRAIN + " ("
			+ KEY_ROWID + " integer primary key autoincrement, " 
			+ KEY_FILENAME + " text)";
	
	Context mCtx;
	DatabaseHelper mDbHelper;
	SQLiteDatabase mDb;
	
	public DatabaseManager(Context ctx) {
		this.mCtx = ctx;
	}
	
	/**
	 * Close the database.
	 */
	public void close() {
		mDbHelper.close();
	}
	
	void destroyTable(String tableName) {
	    try {
	        assert(mDb != null);
    	    mDb.delete(tableName, null, null);
    		mDb.execSQL("DROP TABLE IF EXISTS " + tableName);
	    } catch (Exception ex) {
	        Log.e(TAG, ex.getMessage());
	    }
	}
	
	public SQLiteDatabase getDB() {
		return mDb;
	}
	
	/**
	 * Open the database. If it cannot be opened, try to create a new instance
	 * of the database. If it cannot be created, throw an exception to signal
	 * the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public DatabaseManager open() throws SQLException {
		if (mDbHelper == null || mDb == null || !mDb.isOpen()) {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getWritableDatabase();
			if (!mDb.isOpen()) {
			    resetDatabase();
			}
		}
		return this;
	}
	
	/**
	 * DEBUG: reset database.
	 */
	public void resetDatabase() {
	    destroyTable(TABLE_MAP);
	    destroyTable(TABLE_TERRAIN);
		mDbHelper.onCreate(mDb);
	}
}
