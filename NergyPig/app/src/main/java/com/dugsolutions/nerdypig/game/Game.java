package com.dugsolutions.nerdypig.game;

import android.util.Log;

import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

import java.util.ArrayList;
import java.util.List;

import static com.dugsolutions.nerdypig.MyApplication.TAG;

/**
 * Created by dug on 12/20/16.
 */

public class Game
{
	public enum ResultReport
	{
		ONE_ROLLED, GAME_WON, AI_CONTINUE, AI_STOP, HUMAN_CONTINUE;
	}

	protected int					mTurn;
	protected int					mPlayerScore[];
	protected int					mCurScore;
	protected int					mCurCount;
	protected int					mActivePlayer;
	protected final GameEnd			mGameEnd;
	protected final int				mMaxTurns;
	protected final int				mMaxScore;
	protected ArrayList<Integer>	mRolls	= new ArrayList<>();

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

	public int getTotalScore()
	{
		return getTotalScore(getActivePlayer());
	}

	public int getTotalScore(int i)
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

	public void chkIncTurn()
	{
		if (mActivePlayer == getNumPlayers() - 1)
		{
			mTurn++;
		}
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
	public ResultReport applyRoll(StrategyHolder strategy, int roll)
	{
		mRolls.add(roll);

		if (roll == 1)
		{
			mCurScore = 0;
			return ResultReport.ONE_ROLLED;
		}
		mCurScore += roll;

		if (didWin(getTotalScore() + mCurScore))
		{
			return ResultReport.GAME_WON;
		}
		if (strategy != null)
		{
			if (strategy.getStrategy() == Strategy.STOP_AFTER_NUM_ROLLS)
			{
				mCurCount++;

				if (mCurCount < strategy.getCount())
				{
					return ResultReport.AI_CONTINUE;
				}
				else
				{
					return ResultReport.AI_STOP;
				}
			}
			else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_SUM)
			{
				if (mCurScore < strategy.getCount())
				{
					return ResultReport.AI_CONTINUE;
				}
				else
				{
					return ResultReport.AI_STOP;
				}
			}
			else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_EVEN)
			{
				if (roll % 2 == 0)
				{
					mCurCount++;
				}
				if (mCurCount < strategy.getCount())
				{
					return ResultReport.AI_CONTINUE;
				}
				else
				{
					return ResultReport.AI_STOP;
				}
			}
		}
		return ResultReport.HUMAN_CONTINUE;
	}

	public void applyStop()
	{
		mPlayerScore[mActivePlayer] += mCurScore;
		mCurScore = 0;
		mCurCount = 0;
	}

	public List<Integer> getRolls()
	{
		return mRolls;
	}

	public void clearRolls()
	{
		mRolls.clear();
	}

	public boolean isHuman()
	{
		return isHuman(getActivePlayer());
	}

	public boolean isHuman(int pos)
	{
		if (pos == 0)
		{
			if (GlobalInt.isAIFirst())
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			if (GlobalInt.isAIFirst())
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
}
