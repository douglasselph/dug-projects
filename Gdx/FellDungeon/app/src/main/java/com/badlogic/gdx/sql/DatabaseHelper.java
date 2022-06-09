package com.badlogic.gdx.sql;

import com.badlogic.gdx.Gdx;

public abstract class DatabaseHelper {

	protected static final String TAG = "DatabaseHelper";

	protected Database dbHandler;

	public DatabaseHelper() {
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

	protected void open(String dbName, int dbVersion) {
		StringBuffer sbuf = new StringBuffer();

		create(sbuf);

		dbHandler = DatabaseFactory.getNewDatabase(dbName, dbVersion,
				sbuf.toString(), null);

		dbHandler.setupDatabase();

		try {
			dbHandler.openOrCreateDatabase();
		} catch (SQLiteGdxException e) {
			e.printStackTrace();
		}
	}

	public Database getDB() {
		return dbHandler;
	}

	public DatabaseCursor query(String table, String[] columns, String where) {
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

		if (where != null) {
			sbuf.append(" WHERE ");
			sbuf.append(where);
		}
		DatabaseCursor cursor;

		try {
			cursor = dbHandler.rawQuery(sbuf.toString());
		} catch (SQLiteGdxException e) {
			Gdx.app.error(TAG, e.getMessage());
			return null;
		}
		return cursor;
	}

	public long insert(String table, ContentValues values) {
		try {
			return dbHandler.insert(table, values);
		} catch (Exception ex) {
			Gdx.app.error(TAG, ex.getMessage());
		}
		return 0;
	}

	public void update(String table, ContentValues values, String whereClause) {
		if (values == null || values.size() == 0) {
			throw new IllegalArgumentException("Empty values");
		}
		StringBuilder sql = new StringBuilder(120);
		sql.append("UPDATE ");
		sql.append(table);
		sql.append(" SET ");

		int i = 0;
		for (String colName : values.keySet()) {
			sql.append((i++ > 0) ? "," : "");
			sql.append(colName);
			sql.append("=");
			Object val = values.get(colName);
			if (val instanceof String) {
				sql.append("'");
				sql.append((String) val);
				sql.append("'");
			} else {
				sql.append(val);
			}
		}
		if (whereClause.length() > 0) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		try {
			dbHandler.execSQL(sql.toString());
		} catch (Exception ex) {
			Gdx.app.error(TAG, ex.getMessage());
		}
	}

	public void delete(String table, String whereClause) {
		StringBuilder sql = new StringBuilder(120);
		sql.append("DELETE FROM ");
		sql.append(table);
		if (whereClause != null) {
			sql.append(" ");
			sql.append(whereClause);
		}
		try {
			dbHandler.execSQL(sql.toString());
		} catch (Exception ex) {
			Gdx.app.error(TAG, ex.getMessage());
		}
	}

}
