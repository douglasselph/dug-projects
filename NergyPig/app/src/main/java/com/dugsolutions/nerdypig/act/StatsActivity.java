package com.dugsolutions.nerdypig.act;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.act.MainActivity;
import com.dugsolutions.nerdypig.db.BattleLine;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;
import com.dugsolutions.nerdypig.game.Games;
import com.dugsolutions.nerdypig.game.Player;

import java.util.ArrayList;

public class StatsActivity extends AppCompatActivity
{

	TextView mStats;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stats);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.title_activity_stats);
		setSupportActionBar(toolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

        mStats = (TextView) findViewById(R.id.stats);
        mStats.setMovementMethod(new ScrollingMovementMethod());
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
	protected void onResume()
	{
		super.onResume();

        mStats.setText(run());
	}

	Context getContext()
	{
		return this;
	}

	String run()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getPrelude());

		for (BattleLine.BattleItem battle : BattleLine.getItems())
		{
			Player player = new Player(battle, battle.toString(getContext()));
			ArrayList<Player> list = new ArrayList<>();
			list.add(player);
			Games games = new Games(list);
			games.play();
			sbuf.append(games.toString(getContext()));
		}
		return sbuf.toString();
	}

	public String getPrelude()
	{
		StringBuffer sbuf = new StringBuffer();

		sbuf.append(getString(R.string.game_count, GlobalInt.getNumGames()));
		sbuf.append("\n");
		if (GlobalInt.getGameEnd() == GameEnd.MAX_TURNS)
		{
			sbuf.append(getString(R.string.game_over_turns, GlobalInt.getMaxTurns()));
		}
		else
		{
			sbuf.append(getString(R.string.game_over_points, GlobalInt.getEndPoints()));
		}
		sbuf.append("\n");

		return sbuf.toString();
	}

}
