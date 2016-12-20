package com.dugsolutions.nerdypig.game;

import com.dugsolutions.nerdypig.db.GameEnd;

/**
 * Created by dug on 12/19/16.
 */
public class Game
{
	Games		mMaster;
	int			mTurn;
	int			mPlayerScore[];
	StrategyHolder[]	mUsed;

	public Game(Games master)
	{
		mMaster = master;
		mTurn = 0;
		mPlayerScore = new int[master.getNumPlayers()];
	}

	public void play()
	{
		while (isGameRunning())
		{
			playNextTurn();
		}
	}

	boolean isGameRunning()
	{
		if (mMaster.getGameEnd() == GameEnd.MAX_TURNS)
		{
			return (mTurn < mMaster.getMaxTurns());
		}
		for (int score : mPlayerScore)
		{
			if (score >= mMaster.getMaxScore())
			{
				return false;
			}
		}
		return true;
	}

	void playNextTurn()
	{
		mTurn++;

		mUsed = new StrategyHolder[mMaster.getNumPlayers()];
		boolean gameOver = false;

		for (int playerI = 0; playerI < mMaster.getNumPlayers(); playerI++)
		{
			Player player = mMaster.getPlayer(playerI);
			mUsed[playerI] = player.getStrategy(playerI);

			if (!gameOver)
			{
				int sum = playTurn(mUsed[playerI]);
				mPlayerScore[playerI] += sum;

				if (mMaster.getGameEnd() == GameEnd.END_POINTS && mPlayerScore[playerI] >= mMaster.getMaxScore())
				{
					gameOver = true;
				}
			}
		}
	}

	int playTurn(StrategyHolder strategy)
	{
		int sum = 0;
		int roll;

		if (strategy.getKind() == Strategy.STOP_AFTER_NUM_ROLLS)
		{
			for (int count = 0; count < strategy.getCount(); count++)
			{
				roll = mMaster.getRoll();
				if (roll == 1)
				{
					return 0;
				}
				sum += roll;
			}
		}
		else if (strategy.getKind() == Strategy.STOP_AFTER_REACHED_SUM)
		{
			while (sum < strategy.getCount())
			{
				roll = mMaster.getRoll();
				if (roll == 1)
				{
					return 0;
				}
				sum += roll;
			}
		}
		else if (strategy.getKind() == Strategy.STOP_AFTER_REACHED_EVEN)
		{
			int countEven = 0;

			while (countEven < strategy.getCount())
			{
				roll = mMaster.getRoll();
				if (roll == 1)
				{
					return 0;
				}
				sum += roll;

				if (roll % 2 == 0)
				{
					countEven++;
				}
			}
		}
		return sum;
	}

	public int getWinner()
	{
		int maxScore = 0;
		int winner = -1;

		for (int playerI = 0; playerI < mMaster.getNumPlayers(); playerI++)
		{
			if (mPlayerScore[playerI] > maxScore)
			{
				winner = playerI;
				maxScore = mPlayerScore[playerI];
			}
		}
		return winner;
	}

	public boolean isTie()
	{
		int maxScore = 0;
		boolean isTie = false;

		for (int playerI = 0; playerI < mMaster.getNumPlayers(); playerI++)
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

	public int getTurn()
	{
		return mTurn;
	}

	public int getScore(int i)
	{
		return mPlayerScore[i];
	}

	public StrategyHolder[] getUsed()
	{
		return mUsed;
	}
}
