package com.dugsolutions.nerdypig.act;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
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
import com.dugsolutions.nerdypig.game.StrategyHolder;
import com.dugsolutions.nerdypig.util.DieHelper;

public class BattlePlayActivity extends AppCompatActivity
{
	static final int ROLL = 0;

	class MyHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case ROLL:
					mDieHelper.roll();
					updateRolls(0);
					break;
			}
		}
	}

	static final String		TAG				= MyApplication.TAG;
	static final int		DELAYED_ROLL	= 1000;

	Toolbar					mToolbar;
	TextView				mScore1;
	TextView				mScore2;
	TextView				mDesc1;
	TextView				mDesc2;
	TextView				mCurScore;
	TextView				mGameEndView;
	TextView				mReportView;
	TextView				mPlayer1NameView;
	TextView				mPlayer2NameView;
	FloatingActionButton	mSoundFAB;
	ImageView				mDie;
	DieHelper				mDieHelper;
	Button					mRoll;
	Button					mStop;
	Game					mGame;
	MyApplication			mApp;
	StrategyHolder[]		mPlayer;
	MyHandler				mHandler		= new MyHandler();

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

		mSoundFAB = (FloatingActionButton) findViewById(R.id.fab_sound);
		mSoundFAB.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				GlobalInt.setAudio(!GlobalInt.hasAudio());
				updateSound();
			}
		});
		updateSound();

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
		mReportView = (TextView) findViewById(R.id.report);
		mPlayer1NameView = (TextView) findViewById(R.id.player1_name);
		mPlayer2NameView = (TextView) findViewById(R.id.player2_name);

		mPlayer = new StrategyHolder[2];
		mPlayer[0] = mApp.getPlayer(0);
		mPlayer[1] = mApp.getPlayer(1);

		mDieHelper = new DieHelper(this, mDie, new DieHelper.OnFinished()
		{
			@Override
			public void onFinished(int value)
			{
				applyRoll(value);
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
		mDesc1.setText(mApp.getPlayer(0).getDesc(this));
		mDesc2.setText(mApp.getPlayer(1).getDesc(this));

		setActivePlayer(0);

		if (!isHuman())
		{
			mDieHelper.roll();
		}
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

			if (mGame.getActivePlayer() == 0)
			{
				mPlayer1NameView.setTypeface(null, Typeface.BOLD);
				mPlayer2NameView.setTypeface(null, Typeface.NORMAL);
			}
			else
			{
				mPlayer1NameView.setTypeface(null, Typeface.NORMAL);
				mPlayer2NameView.setTypeface(null, Typeface.BOLD);
			}
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
			mPlayer1NameView.setTypeface(null, Typeface.NORMAL);
			mPlayer2NameView.setTypeface(null, Typeface.NORMAL);
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
		setup();
	}

	void setActivePlayer(int playerI)
	{
		mGame.setActivePlayer(playerI);
		setup();
	}

	void setup()
	{
		mGame.clearRolls();
		updateCurScore();
		updatePlayerTitle();

		if (isHuman())
		{
			showControls();
		}
		else
		{
			hideControls();
		}
	}

	void applyStop()
	{
		mGame.applyStop();
		updateSavedScore();
		setNextActivePlayer();

		if (!isHuman())
		{
			mDieHelper.roll();
		}
	}

	void applyRoll(int value)
	{
		Game.ResultReport result = mGame.applyRoll(getCurStrategy(), value);

		if (result == Game.ResultReport.ONE_ROLLED)
		{
			if (!isHuman())
			{
				updateRolls(R.string.report_one);
			}
			updateCurScore();
			setNextActivePlayer();

			if (!isHuman())
			{
				mHandler.sendEmptyMessageDelayed(0, DELAYED_ROLL);
			}
		}
		else if (mGame.isGameRunning())
		{
			if (isHuman())
			{
				updateCurScore();
				setReport(0);
			}
			else if (result == Game.ResultReport.AI_STOP)
			{
				updateRolls(R.string.report_ai_stop);
				mDieHelper.setPicture(0);
				applyStop();
			}
			else
			{
				mHandler.sendEmptyMessageDelayed(0, DELAYED_ROLL);
			}
		}
		else
		{
			updatePlayerTitle();
			setReport(0);
			mDieHelper.setPicture(0);
			hideControls();
		}
	}

	void setReport(int resId)
	{
		if (resId == 0)
		{
			mReportView.setText("");
		}
		else
		{
			mReportView.setText(getString(resId));
		}
	}

	void hideControls()
	{
		mRoll.setVisibility(View.INVISIBLE);
		mStop.setVisibility(View.INVISIBLE);
	}

	void showControls()
	{
		mRoll.setVisibility(View.VISIBLE);
		mStop.setVisibility(View.VISIBLE);
	}

	void updateRolls(int suffix)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getString(R.string.report_ai_rolls));

		for (int roll : mGame.getRolls())
		{
			sbuf.append(" ");
			sbuf.append(roll);
		}
		if (suffix != 0)
		{
			sbuf.append("\n");
			sbuf.append(getString(suffix));
		}
		mReportView.setText(sbuf.toString());
	}

	void updateSound()
	{
		if (GlobalInt.hasAudio())
		{
			mSoundFAB.setImageResource(R.drawable.sound_on);
		}
		else
		{
			mSoundFAB.setImageResource(R.drawable.sound_off);
		}
	}

}
