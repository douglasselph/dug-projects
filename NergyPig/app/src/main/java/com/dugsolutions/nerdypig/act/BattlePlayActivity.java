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
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;
import com.dugsolutions.nerdypig.game.Game;
import com.dugsolutions.nerdypig.game.StrategyHolder;
import com.dugsolutions.nerdypig.util.DieHelper;

import java.util.ArrayList;
import java.util.List;

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
	TextView				mGameWin;
	TextView				mReport;
	ImageView				mIcon1;
	ImageView				mIcon2;
	ArrayList<ImageView>	mHistory		= new ArrayList<>();
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

		mIcon1 = (ImageView) findViewById(R.id.icon1);
		mIcon2 = (ImageView) findViewById(R.id.icon2);
		mScore1 = (TextView) findViewById(R.id.score1);
		mScore2 = (TextView) findViewById(R.id.score2);
		mDesc1 = (TextView) findViewById(R.id.desc1);
		mDesc2 = (TextView) findViewById(R.id.desc2);
		mCurScore = (TextView) findViewById(R.id.current_score);
		mDie = (ImageView) findViewById(R.id.die);
		mRoll = (Button) findViewById(R.id.roll);
		mStop = (Button) findViewById(R.id.stop);
		mContinue = (Button) findViewById(R.id.ai_continue);
		mGameWin = (TextView) findViewById(R.id.game_win);
		mReport = (TextView) findViewById(R.id.report);
		mHistory.add((ImageView) findViewById(R.id.history1));
		mHistory.add((ImageView) findViewById(R.id.history2));
		mHistory.add((ImageView) findViewById(R.id.history3));
		mHistory.add((ImageView) findViewById(R.id.history4));
		mHistory.add((ImageView) findViewById(R.id.history5));

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
					applyHumanStop();
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
		mGame.setActivePlayer(0);
		mGame.clearRolls();

		updatePlayerTitle();
		updateGameEnd();
		updateGameEnd();
		updateRolls();

		if (!isHuman())
		{
			hideControls();
			mDieHelper.roll();
		}
		else
		{
			updateCurScore();
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

	StrategyHolder getCurStrategy()
	{
		if (mGame.isHuman())
		{
			return null;
		}
		return mPlayer;
	}

	void applyHumanStop()
	{
		mGame.chkIncTurn();
		mGame.applyStop();
		updateSavedScore();
		mGame.setNextActivePlayer();
		updateRolls();
		mGame.clearRolls();
		mDieHelper.reset();

		updatePlayerTitle();
		updateGameEnd();

		showContinue();
	}

	void applyRoll(int value)
	{
		Game.ResultReport result = mGame.applyRoll(getCurStrategy(), value);

		updateCurScore();
		updateRolls();

		Log.d("DEBUG", "applyRoll(" + value + ")");

		if (result == Game.ResultReport.ONE_ROLLED)
		{
			mGame.chkIncTurn();

			setReport(R.string.report_one);

			mGame.setNextActivePlayer();
			mGame.clearRolls();

			updatePlayerTitle();
			updateGameEnd();
			updateCurScore();

			if (isHuman())
			{
				showRollControl();
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
				showControls();
				setReport(0);
			}
			else if (result == Game.ResultReport.GAME_WON)
			{
				setReport(R.string.report_ai_stop);
				mDieHelper.reset();
				mGame.chkIncTurn();
				mGame.applyStop();
				updateSavedScore();
				updateRolls();
				mGame.clearRolls();

				updatePlayerTitle();
				updateGameEnd();

				setReport(R.string.battle_player_ai_won);
				hideControls();
			}
			else if (result == Game.ResultReport.AI_STOP)
			{
				setReport(R.string.report_ai_stop);
				mDieHelper.reset();

				mGame.chkIncTurn();
				mGame.applyStop();
				updateSavedScore();
				mGame.setNextActivePlayer();
				updateRolls();
				mGame.clearRolls();
				mDieHelper.reset();

				updatePlayerTitle();
				updateGameEnd();

				showRollControl();
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
			mDieHelper.reset();
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
			mReport.setText("");
		}
		else
		{
			mReport.setText(getString(resId));
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

	void showRollControl()
	{
		mRoll.setVisibility(View.VISIBLE);
		mStop.setVisibility(View.INVISIBLE);
		mContinue.setVisibility(View.GONE);
	}

	void showContinue()
	{
		mContinue.setText(getString(R.string.ai_turn));
		mContinue.setVisibility(View.VISIBLE);
		mRoll.setVisibility(View.GONE);
		mStop.setVisibility(View.GONE);
		mDie.clearAnimation();
	}

	String getActivePlayerTitle()
	{
		if (mGame.isHuman())
		{
			return getString(R.string.battle_player_your_turn);
		}
		return getString(R.string.battle_player_ai_turn);
	}

	void updatePlayerTitle()
	{
		if (mGame.isGameRunning())
		{
			mToolbar.setTitle(getActivePlayerTitle());

			if (mGame.getActivePlayer() == 0)
			{
				mDesc1.setTypeface(null, Typeface.BOLD);
				mDesc2.setTypeface(null, Typeface.NORMAL);
			}
			else
			{
				mDesc1.setTypeface(null, Typeface.NORMAL);
				mDesc2.setTypeface(null, Typeface.BOLD);
			}
		}
		else
		{
			int winner = mGame.getWinner();
			if (mGame.isHuman(winner))
			{
				mToolbar.setTitle(getString(R.string.battle_player_you_won));
			}
			else
			{
				mToolbar.setTitle(getString(R.string.battle_player_ai_won));
			}
			mDesc1.setTypeface(null, Typeface.NORMAL);
			mDesc2.setTypeface(null, Typeface.NORMAL);
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

	void updateRolls()
	{
		List<Integer> rolls = mGame.getRolls();
		int diePos;
		int dieValue;
		ImageView dieView;

		StringBuffer sbuf = new StringBuffer();
		sbuf.append("updateRolls:");
		for (int roll : rolls)
		{
			sbuf.append(" ");
			sbuf.append(roll);
		}
		Log.d("DEBUG", sbuf.toString());

		if (rolls.size() <= mHistory.size())
		{
			for (diePos = 0; diePos < rolls.size(); diePos++)
			{
				dieValue = rolls.get(diePos);
				dieView = mHistory.get(diePos);

				dieView.setVisibility(View.VISIBLE);
				dieView.setImageLevel(dieValue);
			}
			while (diePos < mHistory.size())
			{
				dieView = mHistory.get(diePos++);
				dieView.setVisibility(View.GONE);
			}
		}
		else
		{
			dieView = mHistory.get(0);
			dieView.setVisibility(View.VISIBLE);
			dieView.setImageLevel(0);

			diePos = rolls.size() - mHistory.size() + 1;
			int viewPos = 1;

			while (diePos < rolls.size())
			{
				dieValue = rolls.get(diePos++);
				dieView = mHistory.get(viewPos++);

				dieView.setVisibility(View.VISIBLE);
				dieView.setImageLevel(dieValue);
			}
		}
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
		mGameWin.setText(sbuf.toString());
	}
}
