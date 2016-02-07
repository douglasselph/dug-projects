package com.dugsolutions.fell.db;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.sql.ContentValues;
import com.badlogic.gdx.sql.DatabaseCursor;
import com.badlogic.gdx.sql.DatabaseHelper;

public class DbCamera {
	static final String TABLE_CAMERA = "camera";
	static final String COLUMN_ID = "_id";
	static final String COLUMN_ENABLE = "enable";
	static final String COLUMN_POS_X = "pos_x";
	static final String COLUMN_POS_Y = "pos_y";
	static final String COLUMN_POS_Z = "pos_z";
	static final String COLUMN_DIR_X = "dir_x";
	static final String COLUMN_DIR_Y = "dir_y";
	static final String COLUMN_DIR_Z = "dir_z";

	static final String DATABASE_CREATE = "create table if not exists "
			+ TABLE_CAMERA + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + COLUMN_ENABLE
			+ " tinyint, " + COLUMN_POS_X + " float, " + COLUMN_POS_Y
			+ " float, " + COLUMN_POS_Z + " float, " + COLUMN_DIR_X
			+ " float, " + COLUMN_DIR_Y + " float, " + COLUMN_DIR_Z
			+ " float, " + COLUMN_ENABLE + " smallint);";

	static void Create(StringBuffer sbuf) {
		sbuf.append(DATABASE_CREATE);
	}

	static ArrayList<DbCamera> query(DatabaseHelper db) {
		DatabaseCursor cursor = db.query(TABLE_CAMERA, null, null);
		int idxId = cursor.getColumnIndex(COLUMN_ID);
		int idxPosX = cursor.getColumnIndex(COLUMN_POS_X);
		int idxPosY = cursor.getColumnIndex(COLUMN_POS_Y);
		int idxPosZ = cursor.getColumnIndex(COLUMN_POS_Z);
		int idxDirX = cursor.getColumnIndex(COLUMN_DIR_X);
		int idxDirY = cursor.getColumnIndex(COLUMN_DIR_Y);
		int idxDirZ = cursor.getColumnIndex(COLUMN_DIR_Z);
		int idxEnable = cursor.getColumnIndex(COLUMN_ENABLE);
		ArrayList<DbCamera> result = new ArrayList<DbCamera>();
		DbCamera cam;

		while (cursor.next()) {
			cam = new DbCamera();
			cam._id = cursor.getLong(idxId);
			cam.pos.x = cursor.getFloat(idxPosX);
			cam.pos.y = cursor.getFloat(idxPosY);
			cam.pos.z = cursor.getFloat(idxPosZ);
			cam.dir.x = cursor.getFloat(idxDirX);
			cam.dir.y = cursor.getFloat(idxDirY);
			cam.dir.z = cursor.getFloat(idxDirZ);
			cam.mEnabled = cursor.getShort(idxEnable) != 0;
			result.add(cam);
		}
		cursor.close();
		return result;
	}

	long _id;
	Vector3 pos;
	Vector3 dir;
	boolean mEnabled;

	public DbCamera() {
		_id = 0;
		pos = new Vector3();
		dir = new Vector3();
	}

	public DbCamera(float px, float py, float pz, float dx, float dy, float dz) {
		_id = 0;
		pos = new Vector3(px, py, pz);
		dir = new Vector3(dx, dy, dz);
	}

	public DbCamera(Vector3 p, Vector3 d) {
		_id = 0;
		pos = new Vector3(p);
		dir = new Vector3(d);
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public Vector3 getPos() {
		return pos;
	}

	public Vector3 getDir() {
		return dir;
	}

	public void setEnabled(boolean flag) {
		mEnabled = flag;
	}

	void update(DatabaseHelper db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_POS_X, pos.x);
		values.put(COLUMN_POS_Y, pos.y);
		values.put(COLUMN_POS_Z, pos.z);
		values.put(COLUMN_DIR_X, dir.x);
		values.put(COLUMN_DIR_Y, dir.y);
		values.put(COLUMN_DIR_Z, dir.z);
		values.put(COLUMN_ENABLE, mEnabled ? 1 : 0);
		if (_id == 0) {
			_id = db.insert(TABLE_CAMERA, values);
		} else {
			db.update(TABLE_CAMERA, values, COLUMN_ID + "=" + _id);
		}
	}

	public void save(DatabaseHelper db, Camera cam) {
		dir.set(cam.direction);
		pos.set(cam.position);
		update(db);
	}

	public void upload(Camera cam) {
		cam.direction.set(dir);
		cam.position.set(pos);
	}

	public boolean equals(Camera cam) {
		return cam.direction.equals(dir) && cam.position.equals(pos);
	}

	public void delete(DatabaseHelper db) {
		StringBuilder where = new StringBuilder();
		where.append("WHERE ");
		where.append(COLUMN_ID);
		where.append("=");
		where.append(_id);
		db.delete(TABLE_CAMERA, where.toString());
	}

}
