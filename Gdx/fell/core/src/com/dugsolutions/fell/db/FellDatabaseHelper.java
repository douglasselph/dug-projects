package com.dugsolutions.fell.db;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.sql.DatabaseHelper;

public class FellDatabaseHelper extends DatabaseHelper {
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

	public void open() {
		super.open(DATABASE_NAME, DATABASE_VERSION);
	}
	
	public void save(Camera cam) {
		DbCamera c = new DbCamera(cam.position, cam.direction);
		c.update(this);
	}
	
	public ArrayList<DbCamera> queryCameras() {
		return DbCamera.query(this);
	}
	
	public void deleteCamera(DbCamera cam) {
		cam.delete(this);
	}

}
