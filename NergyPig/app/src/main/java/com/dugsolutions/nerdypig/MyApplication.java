package com.dugsolutions.nerdypig;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.dugsolutions.nerdypig.act.StatsActivity;
import com.dugsolutions.nerdypig.db.BattleStrategies;
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

	BattleStrategies mPlayer1;
	BattleStrategies mPlayer2;

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

	public void showStatsActivity(Activity act, String action)
	{
		Intent intent = new Intent(this, StatsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(action);
		act.startActivity(intent);
	}

	public void storePlayer1()
	{
		mPlayer1 = new BattleStrategies();
	}

	public void storePlayer2()
	{
		mPlayer2 = new BattleStrategies();
	}

	public void clearPlayers()
	{
		mPlayer1 = null;
		mPlayer2 = null;
	}

	public BattleStrategies getPlayer1()
	{
		return mPlayer1;
	}

	public BattleStrategies getPlayer2()
	{
		return mPlayer2;
	}

}
