package com.dugsolutions.nerdypig.game;

import com.dugsolutions.nerdypig.db.GameEnd;

/**
 * Created by dug on 12/19/16.
 */
public class AutoGame extends Game
{
	AutoGames	mMaster;

	public AutoGame(AutoGames master)
	{
        super(master.getNumPlayers());

		mMaster = master;
	}

	public void play()
	{
		while (isGameRunning())
		{
			playNextTurn();
		}
	}

	void playNextTurn()
	{
		mTurn++;

		for (int playerI = 0; playerI < mMaster.getNumPlayers(); playerI++)
		{
			Player player = mMaster.getPlayer(playerI);

			int sum = playTurn(player.getStrategy(), mPlayerScore[playerI]);
			mPlayerScore[playerI] += sum;

			if (didWin(mPlayerScore[playerI]))
			{
				break;
			}
		}
	}

	int playTurn(StrategyHolder strategy, int curScore)
	{
		int sum = 0;
		int roll;

		if (strategy.getStrategy() == Strategy.STOP_AFTER_NUM_ROLLS)
		{
			for (int count = 0; count < strategy.getCount(); count++)
			{
				roll = mMaster.getRoll();
				if (roll == 1)
				{
					return 0;
				}
				sum += roll;

				if (didWin(curScore + sum))
				{
					break;
				}
			}
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_SUM)
		{
			while (sum < strategy.getCount())
			{
				roll = mMaster.getRoll();
				if (roll == 1)
				{
					return 0;
				}
				sum += roll;

				if (didWin(curScore + sum))
				{
					break;
				}
			}
		}
		else if (strategy.getStrategy() == Strategy.STOP_AFTER_REACHED_EVEN)
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
				if (didWin(curScore + sum))
				{
					break;
				}
			}
		}
		return sum;
	}

}
