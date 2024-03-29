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
	static final String	TAG							= "GlobalInt";

	static final String	TABLE_GLOBALS_INT			= "globals_int";

	static final String	KEY_NAME					= DatabaseManager.KEY_NAME;
	static final String	KEY_ROWID					= DatabaseManager.KEY_ROWID;
	static final String	KEY_VALUE					= DatabaseManager.KEY_VALUE;

	static final String	TABLE_GLOBALS_INT_CREATE	= "create table " + TABLE_GLOBALS_INT + " (" + KEY_ROWID
			+ " integer primary key autoincrement, " + KEY_NAME + " text, " + KEY_VALUE + " integer);";

	static final String	NAME_END_POINTS				= "end_points";
	static final String	NAME_MAX_TURNS				= "max_turns";
	static final String	NAME_NUM_GAMES				= "num_games";
	static final String	NAME_END_TYPE				= "end_type";
	static final String NAME_AUDIO_ON				= "audio_on";
	static final String NAME_BATTLE_AI_FIRST		= "ai_first";

	public static void create(SQLiteDatabase db) throws SQLException
	{
		db.execSQL(TABLE_GLOBALS_INT_CREATE);
	}

	public static void deleteAll()
	{
		DatabaseManager.getDB().delete(TABLE_GLOBALS_INT, null, null);
	}

	static Integer query(String key, int defaultValue)
	{
		Integer value = defaultValue;
		try
		{
			Cursor cursor = DatabaseManager.getDB().query(TABLE_GLOBALS_INT, new String[] {
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
			Log.e(TAG, "KEY=" + key + ": " + ex.getMessage());
		}
		return value;
	}

	static void set(String key, int value)
	{
		SQLiteDatabase db = DatabaseManager.getDB();
		db.beginTransaction();
		try
		{
			setRaw(key, value);
			db.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
		finally
		{
			db.endTransaction();
		}
	}

	static void setRaw(String key, int value)
	{
		SQLiteDatabase db = DatabaseManager.getDB();
		ContentValues values = new ContentValues();
		values.put(KEY_VALUE, value);
		if (db.update(TABLE_GLOBALS_INT, values, KEY_NAME + "=?", new String[] {
				key }) == 0)
		{
			values.put(KEY_NAME, key);
			db.insert(TABLE_GLOBALS_INT, null, values);
		}
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

	static String concat(String prefix, int i)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(prefix);
		sbuf.append(String.valueOf(i));
		return sbuf.toString();
	}
	public static boolean hasAudio()
	{
		return query(NAME_AUDIO_ON, 0) != 0;
	}

	public static void setAudio(boolean value)
	{
		set(NAME_AUDIO_ON, value ? 1 : 0);
	}

	public static boolean isAIFirst()
	{
		return query(NAME_BATTLE_AI_FIRST, 1) != 0;
	}

	public static void setAIFirst(boolean value)
	{
		set(NAME_BATTLE_AI_FIRST, value ? 1 : 0);
	}

}
