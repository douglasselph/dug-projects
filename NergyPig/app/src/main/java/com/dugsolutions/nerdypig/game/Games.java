package com.dugsolutions.nerdypig.game;

import android.content.Context;
import android.util.Log;

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
	static final String TAG = "Games";

	class StrategyCount
	{
		Strategy[]	mStrategies;
		int			mCount;
		int[]		mWin;

		StrategyCount(Game game)
		{
			Strategy[] strategies = game.getUsed();
			mStrategies = new Strategy[strategies.length];
			for (int i = 0; i < strategies.length; i++)
			{
				mStrategies[i] = strategies[i];

				if (strategies[i] == null)
				{
					Log.e(TAG, "ERROR NULL STRATEGY ENCOUNTERED");
				}
			}
			mCount = 1;
			mWin = new int[strategies.length];
			mWin[game.getWinner()] = 1;
		}

		boolean match(Strategy[] strategies)
		{
			if (strategies.length != mStrategies.length)
			{
				return false;
			}
			for (int i = 0; i < strategies.length; i++)
			{
				if (strategies[i] != mStrategies[i])
				{
					return false;
				}
			}
			return true;
		}

		void inc(Game game)
		{
			mCount++;
			mWin[game.getWinner()]++;
		}

		String getDesc(Context ctx)
		{
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("\t");
			for (int i = 0; i < mStrategies.length; i++)
			{
				Strategy strategy = mStrategies[i];

				if (i > 0)
				{
					sbuf.append(" VS\n\t");
				}
				sbuf.append("\t");
				sbuf.append(strategy.toString(ctx));
				sbuf.append(" [WINS=");
				sbuf.append(mWin[i]);
				sbuf.append("]");
			}
			float percent = (float) mCount / (float) mNumGames;
			sbuf.append("\n\t\t\t[USED=");
			sbuf.append((int) (percent * 100));
			sbuf.append("%]");
			return sbuf.toString();
		}
	}

	class PlayerStrategyDesc
	{
		ArrayList<StrategyCount> mList = new ArrayList<>();

		void store(Game game)
		{
			if (mList.size() > 0)
			{
				StrategyCount last = mList.get(mList.size() - 1);
				if (last.match(game.getUsed()))
				{
					last.inc(game);
				}
				else
				{
					mList.add(new StrategyCount(game));
				}
			}
			else
			{
				mList.add(new StrategyCount(game));
			}
		}

		public String getDesc(Context ctx)
		{
			StringBuffer sbuf = new StringBuffer();
			for (StrategyCount sc : mList)
			{
				sbuf.append(sc.getDesc(ctx));
				sbuf.append("\n");
			}
			return sbuf.toString();
		}
	}

	class PlayerStrategyList
	{
		Strategy[] mStrategies;

		PlayerStrategyList(BattleStrategies strategies)
		{
			mStrategies = strategies.getStrategies();
		}
	}

	GameEnd						mGameEnd;
	int							mMaxTurns;
	int							mMaxScore;
	int							mNumGames;
	List<Player>				mPlayers;
	List<PlayerStrategyList>	mStrategies;
	PlayerStrategyDesc			mStrategyDesc;
	Random						mRandom;
	int							mTies;
	int							mGameNumber;

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

	public Games(Context ctx, List<BattleStrategies> players)
	{
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
		mNumGames = GlobalInt.getNumGames();
		mRandom = new Random(System.currentTimeMillis());
		mPlayers = new ArrayList<>();
		mStrategies = new ArrayList<>();
		mStrategyDesc = new PlayerStrategyDesc();

		for (int i = 0; i < players.size(); i++)
		{
			mPlayers.add(new Player(this, ctx.getString(R.string.battle_player, i + 1)));
			mStrategies.add(new PlayerStrategyList(players.get(i)));
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
				mStrategyDesc.store(game);
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
			sbuf.append(getStrategiesDesc(ctx));
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
		return getStrategy(mStrategies.get(playerI).mStrategies);
	}

	Strategy getStrategy(Strategy[] strategies)
	{
		float gamePercent = (float) mGameNumber / (float) mNumGames;
		float strategyF = strategies.length * gamePercent;
		int strategyI = (int) strategyF;
		return strategies[strategyI];
	}

	String getStrategiesDesc(Context ctx)
	{
		return mStrategyDesc.getDesc(ctx);
	}

}
