package com.dugsolutions.nerdypig.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.dugsolutions.nerdypig.MyApplication;

/**
 * Created by dug on 12/18/16.
 */

public class GlobalInt
{
	static final String		TAG							= "GlobalInt";

	static final String		TABLE_GLOBALS_INT			= "globals_int";

	static final String		KEY_NAME					= DatabaseManager.KEY_NAME;
	static final String		KEY_ROWID					= DatabaseManager.KEY_ROWID;
	static final String		KEY_VALUE					= DatabaseManager.KEY_VALUE;

	static final String		TABLE_GLOBALS_INT_CREATE	= "create table " + TABLE_GLOBALS_INT + " (" + KEY_ROWID
			+ " integer primary key autoincrement, " + KEY_NAME + " text, " + KEY_VALUE + " integer);";

	static final String		NAME_END_POINTS				= "end_points";
	static final String		NAME_MAX_TURNS				= "max_turns";
	static final String		NAME_NUM_GAMES				= "num_games";
    static final String     NAME_END_TYPE               = "end_type";

	static SQLiteDatabase	mDb;

	public static void create(SQLiteDatabase db) throws SQLException
	{
		mDb = db;
		db.execSQL(TABLE_GLOBALS_INT_CREATE);
	}

	public static void deleteAll()
	{
		mDb.delete(TABLE_GLOBALS_INT, null, null);
	}

	public static int getEndPoints()
	{
		return query(NAME_END_POINTS, 100);
	}

	public static void setEndPoints(int value)
	{
		set(NAME_END_POINTS, value);
	}

	public static int getMaxTurns()
	{
		return query(NAME_MAX_TURNS, 20);
	}

    public static void setMaxTurns(int value)
    {
        set(NAME_MAX_TURNS, value);
    }


	public static int getNumGames()
	{
		return query(NAME_NUM_GAMES, 1000);
	}

    public static void setNumGames(int value)
    {
        set(NAME_NUM_GAMES, value);
    }

    public static GameEnd getGameEnd()
    {
        return GameEnd.from(query(NAME_END_TYPE, GameEnd.END_POINTS.ordinal()));
    }

    public static void setGameEnd(GameEnd value)
    {
        set(NAME_END_TYPE, value.ordinal());
    }

    static Integer query(String key, int defaultValue)
	{
		Integer value = defaultValue;
		try
		{
			Cursor cursor = mDb.query(TABLE_GLOBALS_INT, new String[] {
					KEY_VALUE }, KEY_NAME + "=?",
					new String[] {
							key },
					null, null, null);
			if (cursor.moveToFirst())
			{
				value = cursor.getInt(0);
			}
			cursor.close();
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
		return value;
	}

	static void set(String key, int value)
	{
		mDb.beginTransaction();
		try
		{
			setRaw(key, value);
			mDb.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	static void setRaw(String key, int value)
	{
		ContentValues values = new ContentValues();
		values.put(KEY_VALUE, value);
		if (mDb.update(TABLE_GLOBALS_INT, values, KEY_NAME + "=?", new String[] {
				key }) == 0)
		{
			values.put(KEY_NAME, key);
			mDb.insert(TABLE_GLOBALS_INT, null, values);
		}
	}

}
