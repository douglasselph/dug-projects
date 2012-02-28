package com.tipsolutions.bugplug;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.tipsolutions.bugplug.data.DatabaseManager;

public class MyApplication extends Application {

	public static final String TAG = "BugPlug";
	public static final Boolean LOG = true;
	
	static DatabaseManager mDb;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
	
	
	public static DatabaseManager getDatabaseManager(Context context) {
		if (mDb == null) {
		    try {
    			mDb = new DatabaseManager(context);
    			mDb.open();
		    } catch (Exception ex) {
		        Log.e(TAG, "FATAL: could not get database\n" + ex.getMessage());
		    }
		}
		return mDb;
	}
}