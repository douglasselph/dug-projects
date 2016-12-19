package com.dugsolutions.nerdypig;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.dugsolutions.nerdypig.db.DatabaseManager;

/**
 * Created by dug on 12/18/16.
 */

public class MyApplication extends Application
{
	public static final String TAG = "NerdyPig";

	public MyApplication()
	{
	}

	/**
	 * This is called when the Home (Up) button is pressed in the Action Bar.
	 */
	public static boolean navigateUp(Activity act, Class claz)
	{
		Intent parentActivityIntent = new Intent(act, claz);
		parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		act.startActivity(parentActivityIntent);
		act.finish();
		return true;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		DatabaseManager.Init(this);
	}
}
