package com.dugsolutions.nerdypig.db;

import com.dugsolutions.nerdypig.game.Strategy;

import java.util.ArrayList;
import java.util.List;

import static com.dugsolutions.nerdypig.db.BattleLine.mSelectedPoints;

/**
 * Created by dug on 12/19/16.
 */

public class BattleStrategy extends Strategy
{
	ArrayList<Short> mAssigned = new ArrayList<>();

	public BattleStrategy(Strategy.Kind type, int count)
    {
        super(type, count);
    }

	public String getId()
	{
		if (mAssigned.size() > 0)
		{
			return String.valueOf(mAssigned.size());
		}
		return "";
	}

	public List<Short> getAssignedTurns()
	{
		return mAssigned;
	}

	public short getMaxTurn()
	{
		short maxTurn = 0;

		for (short turn : mAssigned)
		{
			if (turn > maxTurn)
			{
				maxTurn = turn;
			}
		}
		return maxTurn;
	}

	public int getBattlePoints()
	{
		return mAssigned.size();
	}

	public void incBattlePoints()
	{
		mAssigned.add(new Short(mSelectedPoints++));
	}

	public void clearBattlePoints()
	{
		mAssigned = new ArrayList<>();
	}

	public BattleStrategy dup()
	{
		BattleStrategy item = new BattleStrategy(mType, mCount);
		for (short i : mAssigned)
		{
			item.mAssigned.add(i);
		}
		return item;
	}
}
