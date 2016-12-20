package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dug on 12/19/16.
 */

public class Games
{
	GameEnd			mGameEnd;
	int				mMaxTurns;
	int				mMaxScore;
	int				mNumPlayers;
	int				mNumGames;
	List<Player>	mPlayers;
	Random			mRandom;
	int				mTies;

	public Games(ArrayList<Player> players)
	{
		mNumPlayers = players.size();
		mPlayers = players;
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
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

		for (int count = 0; count < mNumGames; count++)
		{
			Game game = new Game(this);
			game.play();

			if (mNumPlayers > 1)
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
		return mNumPlayers;
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

		if (mNumPlayers > 1)
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
}
