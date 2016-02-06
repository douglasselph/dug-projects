package com.badlogic.gdx.sqlite.android;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.sql.ContentValues;
import com.badlogic.gdx.sql.Database;
import com.badlogic.gdx.sql.DatabaseCursor;
import com.badlogic.gdx.sql.DatabaseManager;
import com.badlogic.gdx.sql.SQLiteGdxException;

/** @author M Rafay Aleem */
public class AndroidDatabaseManager implements DatabaseManager {

	private Context context;

	private class AndroidDatabase implements Database {

		private SQLiteDatabaseHelper helper;
		private SQLiteDatabase database;
		private Context context;

		private final String dbName;
		private final int dbVersion;
		private final String dbOnCreateQuery;
		private final String dbOnUpgradeQuery;

		private AndroidDatabase(Context context, String dbName, int dbVersion,
				String dbOnCreateQuery, String dbOnUpgradeQuery) {
			this.context = context;
			this.dbName = dbName;
			this.dbVersion = dbVersion;
			this.dbOnCreateQuery = dbOnCreateQuery;
			this.dbOnUpgradeQuery = dbOnUpgradeQuery;
		}

		@Override
		public void setupDatabase() {
			helper = new SQLiteDatabaseHelper(this.context, dbName, null,
					dbVersion, dbOnCreateQuery, dbOnUpgradeQuery);
		}

		@Override
		public void openOrCreateDatabase() throws SQLiteGdxException {
			try {
				database = helper.getWritableDatabase();
			} catch (SQLiteException e) {
				throw new SQLiteGdxException(e);
			}
		}

		@Override
		public void closeDatabase() throws SQLiteGdxException {
			try {
				helper.close();
			} catch (SQLiteException e) {
				throw new SQLiteGdxException(e);
			}
		}

		@Override
		public int execSQL(String sql) throws SQLiteGdxException {
			try {
				database.execSQL(sql);
			} catch (SQLException e) {
				throw new SQLiteGdxException(e);
			}
			return 0;
		}

		@Override
		public DatabaseCursor rawQuery(String sql) throws SQLiteGdxException {
			AndroidCursor aCursor = new AndroidCursor();
			try {
				Cursor tmp = database.rawQuery(sql, null);
				aCursor.setNativeCursor(tmp);
				return aCursor;
			} catch (SQLiteException e) {
				throw new SQLiteGdxException(e);
			}
		}

		@Override
		public DatabaseCursor rawQuery(DatabaseCursor cursor, String sql)
				throws SQLiteGdxException {
			AndroidCursor aCursor = (AndroidCursor) cursor;
			try {
				Cursor tmp = database.rawQuery(sql, null);
				aCursor.setNativeCursor(tmp);
				return aCursor;
			} catch (SQLiteException e) {
				throw new SQLiteGdxException(e);
			}
		}

		@Override
		public long insert(String table, ContentValues values)
				throws SQLiteGdxException {
			android.content.ContentValues avalues = new android.content.ContentValues();
			for (String colName : values.keySet()) {
				Object obj = values.get(colName);
				if (obj instanceof String) {
					avalues.put(colName, (String) obj);
				} else if (obj instanceof Long) {
					avalues.put(colName, (Long) obj);
				} else if (obj instanceof Integer) {
					avalues.put(colName, (Integer) obj);
				} else if (obj instanceof Float) {
					avalues.put(colName, (Float) obj);
				} else if (obj instanceof Boolean) {
					avalues.put(colName, (Boolean) obj);
				} else if (obj instanceof Byte) {
					avalues.put(colName, (Byte) obj);
				} else if (obj instanceof Short) {
					avalues.put(colName, (Short) obj);
				} else if (obj instanceof byte[]) {
					avalues.put(colName, (byte[]) obj);
				}
			}
			return database.insert(table, null, avalues);
		}

	}

	public AndroidDatabaseManager() {
		AndroidApplication app = (AndroidApplication) Gdx.app;
		context = app.getApplicationContext();
	}

	@Override
	public Database getNewDatabase(String databaseName, int databaseVersion,
			String databaseCreateQuery, String dbOnUpgradeQuery) {
		return new AndroidDatabase(this.context, databaseName, databaseVersion,
				databaseCreateQuery, dbOnUpgradeQuery);
	}

}
