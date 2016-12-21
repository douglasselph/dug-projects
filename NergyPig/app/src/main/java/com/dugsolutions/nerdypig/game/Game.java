package com.dugsolutions.nerdypig.game;

import android.util.Log;

import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

import static com.dugsolutions.nerdypig.MyApplication.TAG;

/**
 * Created by dug on 12/20/16.
 */

public class Game
{
	protected int			mTurn;
	protected int			mPlayerScore[];
	protected int			mCurScore;
	protected int			mCurCount;
	protected int 			mActivePlayer;
	protected final GameEnd	mGameEnd;
	protected final int		mMaxTurns;
	protected final int		mMaxScore;

	public Game(int numPlayers)
	{
		mPlayerScore = new int[numPlayers];
		mTurn = 0;
		mGameEnd = GlobalInt.getGameEnd();
		mMaxTurns = GlobalInt.getMaxTurns();
		mMaxScore = GlobalInt.getEndPoints();
	}

	public int getTurn()
	{
		return mTurn;
	}

	public int getScore(int i)
	{
		return mPlayerScore[i];
	}

	public boolean isGameRunning()
	{
		if (mGameEnd == GameEnd.MAX_TURNS)
		{
			return (mTurn < mMaxTurns);
		}
		for (int score : mPlayerScore)
		{
			if (score >= mMaxScore)
			{
				return false;
			}
		}
		return true;
	}

	protected void restart()
	{
		for (int playerI = 0; playerI < mPlayerScore.length; playerI++)
		{
			mPlayerScore[playerI] = 0;
		}
	}

	protected boolean didWin(int score)
	{
		return (mGameEnd == GameEnd.END_POINTS) && (score >= mMaxScore);
	}

	public boolean isTie()
	{
		int maxScore = 0;
		boolean isTie = false;

		for (int playerI = 0; playerI < mPlayerScore.length; playerI++)
		{
			if (mPlayerScore[playerI] > maxScore)
			{
				isTie = false;
				maxScore = mPlayerScore[playerI];
			}
			else if (mPlayerScore[playerI] == maxScore)
			{
				isTie = true;
			}
		}
		return isTie;
	}

	public int getWinner()
	{
		int maxScore = 0;
		int winner = -1;

		for (int playerI = 0; playerI < mPlayerScore.length; playerI++)
		{
			if (mPlayerScore[playerI] > maxScore)
			{
				winner = playerI;
				maxScore = mPlayerScore[playerI];
			}
		}
		return winner;
	}

	public int getCurScore()
	{
		return mCurScore;
	}

	public int getActivePlayer()
	{
		return mActivePlayer;
	}

	int getNumPlayers()
	{
		return mPlayerScore.length;
	}

	public void setNextActivePlayer()
	{
		if (++mActivePlayer >= getNumPlayers())
		{
			mActivePlayer = 0;
		}
		mCurCount = 0;
		mCurScore = 0;
	}

	public void setActivePlayer(int i)
	{
		mActivePlayer = i;
		mCurCount = 0;
		mCurScore = 0;
	}

	/**
	 *
	 * @param strategy
	 * @param roll
     * @return true if the player gets another roll.
     */
	public boolean applyRoll(StrategyHolder strategy, int roll)
	{
		if (roll == 1)
		{
			mCurScore = 0;
			Log.d(TAG, "ROLL WAS 1!");
			return false;
		}
		mCurScore += roll;

		Log.d(TAG, "CUR SCORE NOW " + mCurScore);

		if (didWin(mCurScore))
		{
			Log.d(TAG, "DID WIN!");
			return false;
		}
		if (strategy.getStrategy() == Strategy.STOP_AFTER_NUM_ROLLS)
		{
			mCurCount++;
			Log.d(TAG, "CUR COUNT IS NOW " + mCurCount);

			return mCurCount < strategy.getCount();
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_SUM)
		{
			Log.d(TAG, "SCORE IS " + mCurScore + ", STOPPING COUNT IS " + strategy.getCount());

			return mCurScore < strategy.getCount();
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_EVEN)
		{
            if (roll % 2 == 0)
            {
				mCurCount++;
            }
            return mCurCount< strategy.getCount();
		}
		return true;
	}

	public void applyStop()
	{
		mPlayerScore[mActivePlayer] += mCurScore;
		mCurScore = 0;
		mCurCount = 0;
	}

}
