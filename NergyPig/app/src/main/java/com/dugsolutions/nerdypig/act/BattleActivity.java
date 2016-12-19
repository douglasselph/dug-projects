package com.dugsolutions.nerdypig.act;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.BattleLine;

public class BattleActivity extends FragmentActivity implements PlayerFragment.OnListFragmentInteractionListener
{
	static final String TAG = "BattleActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_battle);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.title_activity_battle);
		setActionBar(toolbar);
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.battle_title_player1);
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

	@Override
	public void onListFragmentInteraction(BattleLine.BattleItem item)
	{
		Log.d(TAG, "ITEM SELECTED " + item.toString(this));
	}
}
