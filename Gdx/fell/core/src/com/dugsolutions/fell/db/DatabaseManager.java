package com.dugsolutions.fell.db;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.sql.Database;
import com.badlogic.gdx.sql.DatabaseCursor;
import com.badlogic.gdx.sql.DatabaseFactory;
import com.badlogic.gdx.sql.SQLiteGdxException;

public abstract class DatabaseManager {
	public static final String TAG = "Db";
	public static final String DATABASE_NAME = "fell.db";

	static final int DATABASE_VERSION = 1;

	Database dbHandler;

	public DatabaseManager() {
	}

	public void close() {
		try {
			dbHandler.closeDatabase();
		} catch (SQLiteGdxException e) {
			e.printStackTrace();
		}
		dbHandler = null;
	}

	protected abstract void create(StringBuffer sbuf);

	public Database getDB() {
		return dbHandler;
	}

	protected boolean insert(String table, String column, float value) {
		try {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("INSERT INTO ");
			sbuf.append(table);
			sbuf.append("('");
			sbuf.append(column);
			sbuf.append("')  VALUES (");
			sbuf.append(value);
			sbuf.append(")");
			dbHandler.execSQL(sbuf.toString());
		} catch (SQLiteGdxException e) {
			Gdx.app.error(TAG, e.getMessage());
			return false;
		}
		return true;
	}
	
	void open() {
		StringBuffer sbuf = new StringBuffer();

		create(sbuf);

		dbHandler = DatabaseFactory.getNewDatabase(DATABASE_NAME,
				DATABASE_VERSION, sbuf.toString(), null);

		dbHandler.setupDatabase();

		try {
			dbHandler.openOrCreateDatabase();
		} catch (SQLiteGdxException e) {
			e.printStackTrace();
		}
	}

	protected DatabaseCursor query(String table, String[] columns, String where) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("SELECT ");

		if (columns != null && columns.length > 0) {
			for (String col : columns) {
				sbuf.append(col);
				sbuf.append(" ");
			}
		} else {
			sbuf.append("* ");
		}
		sbuf.append("FROM ");
		sbuf.append(table);

		DatabaseCursor cursor;

		try {
			cursor = dbHandler.rawQuery(sbuf.toString());
		} catch (SQLiteGdxException e) {
			Gdx.app.error(TAG, e.getMessage());
			return null;
		}
		return cursor;
	}
	
	protected boolean update(String table, String column, float value, String where)
	{
		try {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("UPDATE ");
			sbuf.append(table);
			sbuf.append(" SET ");
			sbuf.append(column);
			sbuf.append(" =");
			sbuf.append(value);
			sbuf.append(" WHERE ");
			sbuf.append(where);
			dbHandler.execSQL(sbuf.toString());
		} catch (SQLiteGdxException e) {
			Gdx.app.error(TAG, e.getMessage());
			return false;
		}
		return true;
	}

}
