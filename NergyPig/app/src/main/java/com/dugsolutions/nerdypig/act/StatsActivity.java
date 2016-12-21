package com.dugsolutions.nerdypig.act;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.game.BattleLine;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;
import com.dugsolutions.nerdypig.game.AutoGames;
import com.dugsolutions.nerdypig.game.Player;
import com.dugsolutions.nerdypig.game.StrategyHolder;

public class StatsActivity extends AppCompatActivity
{
	public static final String	ACTION_STATS	= "stats";
	public static final String	ACTION_BATTLE	= "battle";

	class RunGamesTask extends AsyncTask<Integer, Integer, Integer>
	{
		StrategyHolder	mBest;
		double			mBestValue;

		@Override
		protected Integer doInBackground(Integer... params)
		{
			if (getIntent().getAction() == ACTION_BATTLE)
			{
				AutoGames games = new AutoGames(getContext(), mApp.getPlayers());
				games.play();
				Message msg = new Message();
				msg.obj = games.toString(getContext());
				mHandler.sendMessage(msg);
			}
			else
			{
				for (StrategyHolder battle : BattleLine.getItems())
				{
					if (!battle.isHuman())
					{
						Player player = new Player(battle, battle.getName(getContext()));
						AutoGames games = new AutoGames(player);
						games.play();

						Message msg = new Message();
						msg.obj = games.toString(getContext());
						mHandler.sendMessage(msg);

						if (games.getNumPlayers() == 1)
						{
							double value = games.getPlayer(0).getValueAverage();

							if (games.getGameEnd() == GameEnd.MAX_TURNS)
							{
								if (mBest == null || value > mBestValue)
								{
									mBest = battle;
									mBestValue = value;
								}
							}
							else
							{
								if (mBest == null || value < mBestValue)
								{
									mBest = battle;
									mBestValue = value;
								}
							}
						}
					}
				}
			}
			return null;
		}

		@Override
		protected void onCancelled(Integer result)
		{

		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (mBest != null)
			{
				Message msg = new Message();
				StringBuffer sbuf = new StringBuffer();
				sbuf.append(getString(R.string.best));
				sbuf.append(mBest.getDesc(getContext()));
				sbuf.append("\n");
				msg.obj = sbuf.toString();
				mHandler.sendMessage(msg);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute()
		{
			mStats.setText(getPrelude());
			mStats.append("---------\n");
		}
	}

	class MyHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			String str = (String) msg.obj;
			mStats.append(str);
		}
	}

	TextView		mStats;
	MyApplication	mApp;
	RunGamesTask	mRunGames;
	MyHandler		mHandler	= new MyHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();

		setContentView(R.layout.activity_stats);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.title_activity_stats);
		setSupportActionBar(toolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		mStats = (TextView) findViewById(R.id.stats);
		mStats.setMovementMethod(new ScrollingMovementMethod());

		mRunGames = new RunGamesTask();
		mRunGames.execute();
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
	}

	Context getContext()
	{
		return this;
	}

	public String getPrelude()
	{
		StringBuffer sbuf = new StringBuffer();

		sbuf.append(getString(R.string.game_count, GlobalInt.getNumGames()));
		sbuf.append("\n");
		sbuf.append(GlobalInt.getGameEnd().toString(this));
		sbuf.append("\n");

		return sbuf.toString();
	}

}
