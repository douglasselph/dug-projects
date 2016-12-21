package com.dugsolutions.nerdypig.game;

import android.content.Context;
import android.util.Log;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dug on 12/19/16.
 */

public class AutoGames implements AutoGame.InfoQuery
{
	static final String	TAG	= MyApplication.TAG + ".AutoGames";

	GameEnd				mGameEnd;
	int					mMaxTurns;
	int					mMaxScore;
	int					mNumGames;
	List<Player>		mPlayers;
	Random				mRandom;
	int					mTies;
	int					mGameNumber;

	public AutoGames(Player player)
	{
		mPlayers = new ArrayList<>();
		mPlayers.add(player);
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
	}

	public AutoGames(Context ctx, List<StrategyHolder> players)
	{
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
		mPlayers = new ArrayList<>();

		for (int i = 0; i < players.size(); i++)
		{
			mPlayers.add(new Player(players.get(i), ctx.getString(R.string.battle_player, i + 1)));
		}
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
		AutoGame game = new AutoGame(this);

		for (mGameNumber = 0; mGameNumber < mNumGames; mGameNumber++)
		{
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

	public int getRoll()
	{
		return mRandom.nextInt(6) + 1;
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
				sbuf.append(getPlayer(0).getDescAverageScore(ctx));
				sbuf.append("\n");
			}
			else
			{
				sbuf.append(getPlayer(0).getDescAverageTurns(ctx));
				sbuf.append("\n");
			}
		}
		return sbuf.toString();
	}

}
