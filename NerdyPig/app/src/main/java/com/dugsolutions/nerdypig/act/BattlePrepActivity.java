package com.dugsolutions.nerdypig.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.game.BattleLine;
import com.dugsolutions.nerdypig.game.StrategyHolder;

public class BattlePrepActivity extends AppCompatActivity implements PlayerFragment.OnListFragmentInteractionListener
{
	Toolbar       mToolbar;
	MyApplication mApp;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();
		setContentView(R.layout.activity_battle_prep);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setTitle(getString(R.string.battle_title));
		setSupportActionBar(mToolbar);
		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		BattleLine.clearSelected();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
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
		mApp.storePlayer();
		BattleLine.clearSelected();
		showBattleActivity();
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

}
