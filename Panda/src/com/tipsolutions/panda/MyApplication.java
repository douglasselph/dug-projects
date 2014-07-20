package com.tipsolutions.panda;

import android.app.Application;
import android.util.Log;

import com.tipsolutions.panda.database.DatabaseManager;

public class MyApplication extends Application
{
	public static final boolean	LOG					= true;
	public static final String	TAG					= "Panda";

	static final float			STROKE_VIEW_RATIO	= 0.8f;
	DatabaseManager				mDb;

	public float getStrokeViewRatio()
	{
		return STROKE_VIEW_RATIO;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		try
		{
			mDb = new DatabaseManager(this);
			mDb.open();
		}
		catch (Exception ex)
		{
			Log.e(TAG, "FATAL: could not get database\n" + ex.getMessage());
		}
	}

}
