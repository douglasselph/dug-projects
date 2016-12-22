package com.dugsolutions.nerdypig.act;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GameEnd;
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
	Button					mContinue;
	Game					mGame;
	MyApplication			mApp;
	StrategyHolder			mPlayer;
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
		mContinue = (Button) findViewById(R.id.ai_continue);
		mGameEndView = (TextView) findViewById(R.id.game_win);
		mReportView = (TextView) findViewById(R.id.report);
		mPlayer1NameView = (TextView) findViewById(R.id.player1_name);
		mPlayer2NameView = (TextView) findViewById(R.id.player2_name);

		mPlayer = mApp.getPlayer();

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
					setReport(0);
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
		mContinue.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				setReport(0);
				hideControls();
				mDieHelper.roll();
			}
		});
		mGame = new Game(2);

		if (GlobalInt.isAIFirst())
		{
			mDesc1.setText(mApp.getPlayer().getDesc(this));
			mDesc2.setText(R.string.you);
		}
		else
		{
			mDesc2.setText(mApp.getPlayer().getDesc(this));
			mDesc1.setText(R.string.you);
		}
		setActivePlayer(0);

		if (!isHuman())
		{
			hideControls();
			mDieHelper.roll();
		}
		updateGameEnd();
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

	StrategyHolder getCurStrategy()
	{
		if (mGame.isHuman())
		{
			return null;
		}
		return mPlayer;
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
		updatePlayerTitle();
		updateGameEnd();

		if (isHuman())
		{
			updateCurScore();
		}
	}

	void applyStop()
	{
		mGame.chkIncTurn();
		mGame.applyStop();
		updateSavedScore();
		setNextActivePlayer();
		mDieHelper.setPicture(0);

		if (isHuman())
		{
			showControls();
		}
		else
		{
			showContinue();
		}
	}

	void applyRoll(int value)
	{
		Game.ResultReport result = mGame.applyRoll(getCurStrategy(), value);

		updateCurScore();

		if (result == Game.ResultReport.ONE_ROLLED)
		{
			mGame.chkIncTurn();

			if (isHuman())
			{
				updateRolls(0);
				setReport(R.string.report_one);
			}
			else
			{
				updateRolls(R.string.report_one);
			}
			setNextActivePlayer();

			if (isHuman())
			{
				showControls();
			}
			else
			{
				showContinue();
			}
		}
		else if (mGame.isGameRunning())
		{
			if (isHuman())
			{
				setReport(0);
			}
			else if (result == Game.ResultReport.GAME_WON)
			{
				updateRolls(R.string.report_ai_stop);
				mDieHelper.setPicture(0);
				applyStop();
				setReport(0);
				hideControls();
				updatePlayerTitle();

			}
			else if (result == Game.ResultReport.AI_STOP)
			{
				updateRolls(R.string.report_ai_stop);
				mDieHelper.setPicture(0);
				applyStop();
			}
			else
			{
				updateRolls(0);
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

	boolean isHuman()
	{
		return mGame.isHuman();
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
		mContinue.setVisibility(View.GONE);
	}

	void showControls()
	{
		mRoll.setVisibility(View.VISIBLE);
		mStop.setVisibility(View.VISIBLE);
		mContinue.setVisibility(View.GONE);
	}

	void showContinue()
	{
		mContinue.setText(getString(R.string.ai_turn));
		mContinue.setVisibility(View.VISIBLE);
		mRoll.setVisibility(View.GONE);
		mStop.setVisibility(View.GONE);
	}

	String getActivePlayerDesc()
	{
		return getString(R.string.battle_player_turn, mGame.getActivePlayer() + 1);
	}

	void updatePlayerTitle()
	{
		if (mGame.isGameRunning())
		{
			mToolbar.setTitle(getActivePlayerDesc());

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
		mScore1.setText(Integer.toString(mGame.getTotalScore(0)));
		mScore2.setText(Integer.toString(mGame.getTotalScore(1)));
	}

	void updateCurScore()
	{
		mCurScore.setText(Integer.toString(mGame.getCurScore()));
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

	void updateGameEnd()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(GlobalInt.getGameEnd().toString(this));

		if (GlobalInt.getGameEnd() == GameEnd.MAX_TURNS)
		{
			sbuf.append(" [TURN ");
			sbuf.append(mGame.getTurn() + 1);
			sbuf.append("]");
		}
		mGameEndView.setText(sbuf.toString());
	}
}
