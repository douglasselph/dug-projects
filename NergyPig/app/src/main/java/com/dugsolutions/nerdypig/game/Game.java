package com.dugsolutions.nerdypig.game;

import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

/**
 * Created by dug on 12/20/16.
 */

public class Game
{
	protected int			mTurn;
	protected int			mPlayerScore[];
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

	protected boolean isGameRunning()
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

	public void addScore(int playerI, int roll)
	{
		mPlayerScore[playerI] += roll;
	}

	public boolean applyRoll(StrategyHolder strategy, int roll)
	{
		if (roll == 1)
		{
			GlobalInt.setCurScore(0);
			return false;
		}
		GlobalInt.setCurScore(GlobalInt.getCurScore() + roll);

		if (strategy.getStrategy() == Strategy.STOP_AFTER_NUM_ROLLS)
		{
            GlobalInt.setCurCount(GlobalInt.getCurCount() + 1);
            return GlobalInt.getCurCount() < strategy.getCount();
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_SUM)
		{
            GlobalInt.setCurCount(GlobalInt.getCurCount() + 1);
            return GlobalInt.getCurScore() < strategy.getCount();
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_EVEN)
		{
            if (roll % 2 == 0)
            {
                GlobalInt.setCurCount(GlobalInt.getCurCount() + 1);
            }
            return GlobalInt.getCurCount() < strategy.getCount();
		}
		return true;
	}

}
