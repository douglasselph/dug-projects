package com.dugsolutions.nerdypig.act;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

	Toolbar		mToolbar;
	TextView	mScore1;
	TextView	mScore2;
	TextView	mCurScore;
	ImageView	mDie;
	DieHelper	mDieHelper;
	Button		mRoll;
	Button		mStop;
	Game		mGame;
	MyApplication mApp;

	StrategyHolder [] mPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mApp = (MyApplication) getApplicationContext();

		setContentView(R.layout.activity_battle_play);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);

		mScore1 = (TextView) findViewById(R.id.player1_score);
		mScore2 = (TextView) findViewById(R.id.player2_score);
		mCurScore = (TextView) findViewById(R.id.current_score);
		mDie = (ImageView) findViewById(R.id.die);
		mRoll = (Button) findViewById(R.id.roll);
		mStop = (Button) findViewById(R.id.stop);

		mPlayer = new StrategyHolder[2];
		mPlayer[0] = mApp.getPlayer(0);
		mPlayer[1] = mApp.getPlayer(1);

		mDieHelper = new DieHelper(this, mDie, new DieHelper.OnFinished()
		{
			@Override
			public void onFinished(int value)
			{
				mGame.applyRoll(mPlayer[GlobalInt.getActivePlayer()], value);
			}
		});

		mRoll.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mDieHelper.roll();
			}
		});

		mStop.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

			}
		});

		mGame = new Game(2);

		updateTitle();
	}

	void updateTitle()
	{
		mToolbar.setTitle(getString(R.string.battle_player_turn, GlobalInt.getActivePlayer()));
	}

	void updateSavedScore()
	{
		mScore1.setText(Integer.toString(GlobalInt.getSavedScore(0)));
		mScore2.setText(Integer.toString(GlobalInt.getSavedScore(1)));
	}

	void updateCurScore()
	{
		mCurScore.setText(Integer.toString(GlobalInt.getCurScore()));
	}

}
