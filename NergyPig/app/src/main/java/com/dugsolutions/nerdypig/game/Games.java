package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.BattleStrategies;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dug on 12/19/16.
 */

public class Games implements Player.QueryStrategy
{
	GameEnd			mGameEnd;
	int				mMaxTurns;
	int				mMaxScore;
	int				mNumGames;
	List<Player>	mPlayers;
	Strategy[]		mStrategies1;
	Strategy[]		mStrategies2;
	Random			mRandom;
	int				mTies;
	int 			mGameNumber;

	public Games(Player player)
	{
		mPlayers = new ArrayList<>();
		mPlayers.add(player);
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
	}

	public Games(Context ctx, BattleStrategies player1, BattleStrategies player2)
	{
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
		mPlayers = new ArrayList<>();
		mPlayers.add(new Player(this, ctx.getString(R.string.battle_player, 1)));
		mPlayers.add(new Player(this, ctx.getString(R.string.battle_player, 2)));
		mStrategies1 = player1.getStrategies();
		mStrategies2 = player2.getStrategies();
	}

	void reset()
	{
		for (Player player : mPlayers)
		{
			player.reset();
		}
	}

	public void play()
	{
		reset();
		mTies = 0;

		for (mGameNumber = 0; mGameNumber < mNumGames; mGameNumber++)
		{
			Game game = new Game(this);
			game.play();

			if (mPlayers.size() > 1)
			{
				if (game.isTie())
				{
					mTies++;
				}
				else
				{
					getPlayer(game.getWinner()).win();
				}
			}
			else
			{
				if (mGameEnd == GameEnd.END_POINTS)
				{
					getPlayer(0).addValue(game.getTurn());
				}
				else
				{
					getPlayer(0).addValue(game.getScore(0));
				}
			}
		}
	}

	public int getNumPlayers()
	{
		return mPlayers.size();
	}

	public GameEnd getGameEnd()
	{
		return mGameEnd;
	}

	public int getMaxTurns()
	{
		return mMaxTurns;
	}

	public int getMaxScore()
	{
		return mMaxScore;
	}

	public Player getPlayer(int i)
	{
		return mPlayers.get(i);
	}

	public int getRoll()
	{
		return mRandom.nextInt(6) + 1;
	}

	public String toString(Context ctx)
	{
		StringBuffer sbuf = new StringBuffer();

		if (mPlayers.size() > 1)
		{
			for (Player player : mPlayers)
			{
				sbuf.append(player.getDescGamesWon(mNumGames));
				sbuf.append("\n");
			}
			if (mTies > 0)
			{
				sbuf.append(ctx.getString(R.string.report_tie, mTies));
				sbuf.append("\n");
			}
		}
		else
		{
			if (mGameEnd == GameEnd.MAX_TURNS)
			{
				for (Player player : mPlayers)
				{
					sbuf.append(player.getDescAverageScore(ctx));
					sbuf.append("\n");
				}
			}
			else
			{
				for (Player player : mPlayers)
				{
					sbuf.append(player.getDescAverageTurns(ctx));
					sbuf.append("\n");
				}
			}
		}
		return sbuf.toString();
	}

	@Override
	public Strategy getStrategy(int playerI)
	{
		if (playerI == 0)
		{
			return getStrategy(mStrategies1);
		}
		return getStrategy(mStrategies2);
	}

	Strategy getStrategy(Strategy [] strategies)
	{
		float gamePercent = (float) mGameNumber / (float) mNumGames;
		float strategyF = strategies.length * gamePercent;
		int strategyI = (int) strategyF;
		return strategies[strategyI];
	}
}
