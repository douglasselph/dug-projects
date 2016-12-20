package com.dugsolutions.nerdypig.act;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.BattleLine;
import com.dugsolutions.nerdypig.db.BattleStrategy;
import com.dugsolutions.nerdypig.db.GlobalInt;

public class BattleActivity extends FragmentActivity implements PlayerFragment.OnListFragmentInteractionListener
{
	static final String	TAG	= "BattleActivity";

	int					mBattlePointsLeft;
	Toolbar				mToolbar;
	MyApplication		mApp;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();
		setContentView(R.layout.activity_battle);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setActionBar(mToolbar);
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		BattleLine.clearBattlePoints();
		mBattlePointsLeft = GlobalInt.getBattlePoints();
		mApp.clearPlayers();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		updateTitle();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();

		if (itemId == android.R.id.home)
		{
			return MyApplication.navigateUp(this, MainActivity.class);
		}
		return super.onOptionsItemSelected(item);
	}

	// Return true if entire data set has changed.
	@Override
	public boolean onListFragmentInteraction(BattleStrategy item)
	{
		boolean refresh = false;

		mBattlePointsLeft--;

		if (mBattlePointsLeft <= 0)
		{
			refresh = true;

			if (mApp.getPlayer(0) == null)
			{
				mApp.storePlayer();
				BattleLine.clearBattlePoints();
				mBattlePointsLeft = GlobalInt.getBattlePoints();
				updateTitle();
			}
			else
			{
				mApp.storePlayer();
				showStatsActivity();
			}
		}
		else
		{
			updateTitle();
		}
		return refresh;
	}

	void updateTitle()
	{
		if (mApp.getPlayer(0) == null)
		{
			mToolbar.setTitle(getString(R.string.battle_title_player, 1, mBattlePointsLeft));
		}
		else
		{
			mToolbar.setTitle(getString(R.string.battle_title_player, 2, mBattlePointsLeft));
		}
	}

	void showStatsActivity()
	{
		mApp.showStatsActivity(this, StatsActivity.ACTION_BATTLE);
	}
}
