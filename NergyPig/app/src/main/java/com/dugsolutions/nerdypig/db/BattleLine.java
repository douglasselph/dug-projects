package com.dugsolutions.nerdypig.db;

import com.dugsolutions.nerdypig.game.Strategy;

import java.util.ArrayList;
import java.util.List;

public class BattleLine
{
	public static List<BattleStrategy> getItems()
	{
		return ITEMS;
	}

	static short mSelectedPoints;

	public static void clearBattlePoints()
	{
		for (BattleStrategy item : ITEMS)
		{
			item.clearBattlePoints();
		}
		mSelectedPoints = 0;
	}

	static final List<BattleStrategy> ITEMS = new ArrayList<>();

	static
	{
		for (int rolls = 3; rolls <= 7; rolls++)
		{
			addItem(createItem(Strategy.Kind.STOP_AFTER_NUM_ROLLS, rolls));
		}
		for (int sum = 20; sum < 30; sum++)
		{
			addItem(createItem(Strategy.Kind.STOP_AFTER_REACHED_SUM, sum));
		}
		addItem(createItem(Strategy.Kind.STOP_AFTER_REACHED_EVEN, 2));
	}

	private static void addItem(BattleStrategy item)
	{
		ITEMS.add(item);
	}

	private static BattleStrategy createItem(Strategy.Kind type, int count)
	{
		return new BattleStrategy(type, count);
	}

	public static List<BattleStrategy> getSelectedBattleLines()
	{
		ArrayList<BattleStrategy> list = new ArrayList<>();

		for (BattleStrategy item : ITEMS)
		{
			if (item.getBattlePoints() > 0)
			{
				list.add(item.dup());
			}
		}
		return list;
	}

}
