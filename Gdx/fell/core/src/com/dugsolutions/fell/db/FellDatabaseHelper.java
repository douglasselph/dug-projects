package com.dugsolutions.fell.db;

import com.badlogic.gdx.sql.DatabaseHelper;

public abstract class FellDatabaseHelper extends DatabaseHelper {
	public static final String TAG = "Db";
	public static final String DATABASE_NAME = "fell.db";

	static final int DATABASE_VERSION = 1;

	public FellDatabaseHelper() {
		super();
	}

	@Override
	protected void create(StringBuffer sbuf) {
		DbCamera.Create(sbuf);
	}

	void open() {
		super.open(DATABASE_NAME, DATABASE_VERSION);
	}

}
