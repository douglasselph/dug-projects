package com.dugsolutions.fell.db;

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
			+ " float); ";

	static void Create(StringBuffer sbuf) {
		sbuf.append(DATABASE_CREATE);
	}

	long _id;
	float pos_x;
	float pos_y;
	float pos_z;
	float dir_x;
	float dir_y;
	float dir_z;
	boolean mEnabled;

	public DbCamera(float px, float py, float pz, float dx, float dy, float dz) {
		pos_x = px;
		pos_y = py;
		pos_z = pz;
		dir_x = dx;
		dir_y = dy;
		dir_z = dz;
	}

	public float getPosX() {
		return pos_x;
	}

	public float getPosY() {
		return pos_y;
	}

	public float getPosZ() {
		return pos_z;
	}

	public float getDirX() {
		return dir_x;
	}

	public float getDirY() {
		return dir_y;
	}

	public float getDirZ() {
		return dir_z;
	}
	
	void update(FellDatabaseHelper db) {
		if (_id == 0) {
		}
	}

}
