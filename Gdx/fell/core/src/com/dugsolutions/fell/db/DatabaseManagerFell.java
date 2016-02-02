package com.dugsolutions.fell.db;

public class DatabaseManagerFell extends DatabaseManager {

	@Override
	protected void create(StringBuffer sbuf) {
		DbCamera.Create(sbuf);
	}

}
