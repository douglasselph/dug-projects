package com.dugsolutions.bugplug;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.dugsolutions.bugplug.map.DatabaseManager;
import com.dugsolutions.jacket.image.TextureManager;

public class MyApplication extends Application
{

	public static final String	TAG	= "BugPlug";
	public static final Boolean	LOG	= true;

	static DatabaseManager		mDb;
	static TextureManager		mTM;

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	public static DatabaseManager getDatabaseManager(Context context)
	{
		if (mDb == null)
		{
			try
			{
				mDb = new DatabaseManager(context);
				mDb.open();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "FATAL: could not get database\n" + ex.getMessage());
			}
		}
		return mDb;
	}

	public static TextureManager getTM(Context context)
	{
		if (mTM == null)
		{
			mTM = new TextureManager(context);
		}
		return mTM;
	}
}
