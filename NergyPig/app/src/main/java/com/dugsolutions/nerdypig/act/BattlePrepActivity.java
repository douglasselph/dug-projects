package com.dugsolutions.nerdypig.act;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.game.BattleLine;
import com.dugsolutions.nerdypig.game.StrategyHolder;

public class BattlePrepActivity extends AppCompatActivity implements PlayerFragment.OnListFragmentInteractionListener
{
	Toolbar				mToolbar;
	MyApplication		mApp;
	TextView			mPlayer1Name;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();
		setContentView(R.layout.activity_battle_prep);
		mPlayer1Name = (TextView) findViewById(R.id.player1_name);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		BattleLine.clearSelected();
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
	public void onListFragmentInteraction(StrategyHolder item)
	{
		if (mApp.getPlayer(0) == null)
		{
			mApp.storePlayer();
			BattleLine.clearSelected();
			updateTitle();
			updatePlayer1Text();
		}
		else
		{
			mApp.storePlayer();


			if (mApp.hasHuman())
			{
				showBattleActivity();
			}
			else
			{
				showStatsActivity();
			}
		}
	}

	void updateTitle()
	{
		if (mApp.getPlayer(0) == null)
		{
			mToolbar.setTitle(getString(R.string.battle_title_player, 1));
		}
		else
		{
			mToolbar.setTitle(getString(R.string.battle_title_player, 2));
		}
	}

	void showStatsActivity()
	{
		mApp.showStatsActivity(this, StatsActivity.ACTION_BATTLE);
	}

	public void showBattleActivity()
	{
		Intent intent = new Intent(this, BattlePlayActivity.class);
		startActivity(intent);
	}

	void updatePlayer1Text()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getString(R.string.player_1));
		sbuf.append(": ");
		sbuf.append(mApp.getPlayer(0).getDesc(this));

		mPlayer1Name.setText(sbuf.toString());
		mPlayer1Name.setVisibility(View.VISIBLE);
	}
}
