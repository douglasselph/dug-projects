package com.dugsolutions.nerdypig;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.dugsolutions.nerdypig.act.StatsActivity;
import com.dugsolutions.nerdypig.battle.BattleStrategies;
import com.dugsolutions.nerdypig.db.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dug on 12/18/16.
 */

public class MyApplication extends Application
{
	public static final String	TAG			= "NerdyPig";

	static final String			EMAIL_TO	= "douglasselph@gmail.com";

	public MyApplication()
	{
	}

	ArrayList<BattleStrategies> mPlayers = new ArrayList<>();

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

	public void storePlayer()
	{
		mPlayers.add(new BattleStrategies());
	}

	public void clearPlayers()
	{
		mPlayers.clear();
	}

	public List<BattleStrategies> getPlayers()
	{
		return mPlayers;
	}

	public BattleStrategies getPlayer(int i)
	{
		if (i >= mPlayers.size())
		{
			return null;
		}
		return mPlayers.get(i);
	}

	public String getVersion()
	{
		StringBuilder sbuf = new StringBuilder();
		try
		{
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			sbuf.append("v");
			sbuf.append(version);
		}
		catch (Exception e)
		{

			Log.i(TAG, e.getMessage());

		}
		return sbuf.toString();
	}

	public void doEmail(Activity act)
	{
		doEmail(act, EMAIL_TO);
	}

	public void doEmail(Activity act, String to)
	{
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setData(Uri.parse("mailto:"));
		emailIntent.setType("text/plain");
		if (to != null)
		{
			String[] TO = {
					to };
			emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		}
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
		emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));

		try
		{
			act.startActivity(Intent.createChooser(emailIntent, getString(R.string.email_subject)));
		}
		catch (android.content.ActivityNotFoundException ex)
		{
			Log.e(TAG, ex.getMessage());
			Toast.makeText(act, getString(R.string.email_failed), Toast.LENGTH_SHORT).show();
		}
	}

}
