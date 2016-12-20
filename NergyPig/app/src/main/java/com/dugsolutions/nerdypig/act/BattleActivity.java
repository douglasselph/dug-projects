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
import com.dugsolutions.nerdypig.db.GlobalInt;

public class BattleActivity extends FragmentActivity implements PlayerFragment.OnListFragmentInteractionListener
{
	static final String TAG = "BattleActivity";

	int mBattlePointsLeft;
	Toolbar mToolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_battle);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setActionBar(mToolbar);
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		mBattlePointsLeft = GlobalInt.getBattlePoints();
	}

	@Override
	protected void onResume() {
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

	@Override
	public void onListFragmentInteraction(BattleLine.BattleItem item)
	{
		mBattlePointsLeft--;
		updateTitle();
	}

	void updateTitle()
	{
		mToolbar.setTitle(getString(R.string.battle_title_player1, mBattlePointsLeft));
	}
}
