package com.dugsolutions.nerdypig.game;

import android.util.Log;

import com.dugsolutions.nerdypig.db.GameEnd;

import static com.dugsolutions.nerdypig.MyApplication.TAG;

/**
 * Created by dug on 12/19/16.
 */
public class AutoGame extends Game
{
	public interface InfoQuery
	{
		int getNumPlayers();

		Player getPlayer(int i);

		int getRoll();
	}

	final InfoQuery	mQuery;
	Player[]		mPlayers;

	public AutoGame(InfoQuery query)
	{
		super(query.getNumPlayers());
		mQuery = query;
        mPlayers = new Player[mQuery.getNumPlayers()];
    }

	public void play()
	{

		for (int playerI = 0; playerI < mPlayers.length; playerI++)
		{
			mPlayers[playerI] = mQuery.getPlayer(playerI);

			if (mPlayers[playerI].getStrategy().isHuman())
			{
				Log.e(TAG, "Invalid HUMAN strategy in auto game play");
				return;
			}
		}
        mTurn = 0;
        restart();

		while (isGameRunning())
		{
			playNextTurn();
		}
	}

	void playNextTurn()
	{
		mTurn++;

		for (int playerI = 0; playerI < mQuery.getNumPlayers(); playerI++)
		{
			Player player = mPlayers[playerI];

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
				roll = mQuery.getRoll();
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
				roll = mQuery.getRoll();
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
				roll = mQuery.getRoll();
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
