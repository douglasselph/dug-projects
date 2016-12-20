package com.dugsolutions.nerdypig.db;

import android.util.Log;

import com.dugsolutions.nerdypig.game.Strategy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dug on 12/19/16.
 */

public class BattleStrategies
{
	static final String			TAG	= "BattleStrategies";

	List<BattleStrategy>	mStrategies;

	public BattleStrategies()
	{
		mStrategies = BattleLine.getSelectedBattleLines();
	}

	public Strategy[] getStrategies()
	{
		int maxGames = 0;

		for (BattleStrategy item : mStrategies)
		{
			if (item.getMaxTurn() > maxGames)
			{
				maxGames = item.getMaxTurn();
			}
		}
		Strategy[] strategies = new Strategy[maxGames+1];

		for (BattleStrategy item : mStrategies)
		{
			for (short game : item.getAssignedTurns())
			{
				if (strategies[game] != null)
				{
					Log.e(TAG, "More than one strategy for game " + game);
				}
				else
				{
					strategies[game] = item;
				}
			}
		}
        return strategies;
	}

}
