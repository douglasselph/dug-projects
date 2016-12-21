package com.dugsolutions.nerdypig.act;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GlobalInt;
import com.dugsolutions.nerdypig.game.Game;
import com.dugsolutions.nerdypig.game.Strategy;
import com.dugsolutions.nerdypig.game.StrategyHolder;
import com.dugsolutions.nerdypig.util.DieHelper;

public class BattlePlayActivity extends AppCompatActivity
{
	static final String TAG = MyApplication.TAG;

	Toolbar				mToolbar;
	TextView			mScore1;
	TextView			mScore2;
	TextView			mDesc1;
	TextView			mDesc2;
	TextView			mCurScore;
	TextView			mGameEndView;
	ImageView			mDie;
	DieHelper			mDieHelper;
	Button				mRoll;
	Button				mStop;
	Game				mGame;
	MyApplication		mApp;
	StrategyHolder[]	mPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();

		setContentView(R.layout.activity_battle_play);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		mScore1 = (TextView) findViewById(R.id.player1_score);
		mScore2 = (TextView) findViewById(R.id.player2_score);
		mDesc1 = (TextView) findViewById(R.id.player1_desc);
		mDesc2 = (TextView) findViewById(R.id.player2_desc);
		mCurScore = (TextView) findViewById(R.id.current_score);
		mDie = (ImageView) findViewById(R.id.die);
		mRoll = (Button) findViewById(R.id.roll);
		mStop = (Button) findViewById(R.id.stop);
		mGameEndView = (TextView) findViewById(R.id.game_win);
		mGameEndView.setText(GlobalInt.getGameEnd().toString(this));

		mPlayer = new StrategyHolder[2];
		mPlayer[0] = mApp.getPlayer(0);
		mPlayer[1] = mApp.getPlayer(1);

		mDieHelper = new DieHelper(this, mDie, new DieHelper.OnFinished()
		{
			@Override
			public void onFinished(int value)
			{
				Log.d(TAG, "ROLL=" + value);

				if (mGame.applyRoll(getCurStrategy(), value))
				{
					checkAutoRoll();
					updateCurScore();
				}
				else if (mGame.isGameRunning())
				{
					if (!isHuman())
					{
						applyStop();
					}
					else
					{
						updateCurScore();
					}
				}
				else
				{
					updatePlayerTitle();
				}
			}
		});

		mRoll.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mGame.isGameRunning() && isHuman())
				{
					mDieHelper.roll();
				}
			}
		});

		mStop.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mGame.isGameRunning() && isHuman())
				{
					applyStop();
				}
			}
		});
		mGame = new Game(2);

		setActivePlayer(0);

		mDesc1.setText(mApp.getPlayer(0).getDesc(this));
		mDesc2.setText(mApp.getPlayer(1).getDesc(this));
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

	void updatePlayerTitle()
	{
		if (mGame.isGameRunning())
		{
			mToolbar.setTitle(getString(R.string.battle_player_turn, mGame.getActivePlayer() + 1));
		}
		else
		{
			int winner = mGame.getWinner();
			if (winner >= 0)
			{
				mToolbar.setTitle(getString(R.string.battle_player_wins, winner + 1));
			}
			else
			{
				mToolbar.setTitle("");
			}
		}
	}

	void updateSavedScore()
	{
		mScore1.setText(Integer.toString(mGame.getScore(0)));
		mScore2.setText(Integer.toString(mGame.getScore(1)));
	}

	void updateCurScore()
	{
		mCurScore.setText(Integer.toString(mGame.getCurScore()));
	}

	StrategyHolder getCurStrategy()
	{
		return mPlayer[mGame.getActivePlayer()];
	}

	boolean isHuman()
	{
		return getCurStrategy().isHuman();
	}

	void setNextActivePlayer()
	{
		mGame.setNextActivePlayer();
		updateCurScore();
		updatePlayerTitle();
		checkAutoRoll();
	}

	void setActivePlayer(int playerI)
	{
		mGame.setActivePlayer(playerI);
		mDieHelper.setPicture(0);
		updateCurScore();
		updatePlayerTitle();
		checkAutoRoll();
	}

	void checkAutoRoll()
	{
		if (!isHuman())
		{
			mDieHelper.roll();
		}
	}

	void applyStop()
	{
		mGame.applyStop();
		updateSavedScore();
		setNextActivePlayer();
	}
}
