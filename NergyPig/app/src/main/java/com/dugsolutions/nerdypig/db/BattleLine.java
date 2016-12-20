package com.dugsolutions.nerdypig.db;

import com.dugsolutions.nerdypig.game.Strategy;

import java.util.ArrayList;
import java.util.List;

public class BattleLine
{
	/**
	 * A dummy item representing a piece of content.
	 */
	public static class BattleItem extends Strategy
	{
		short		mAssigned;	// How many battle points assigned to this strategy

		public BattleItem(Kind type, int count)
		{
			super(type, count);
		}

		public String getId()
		{
			if (mAssigned > 0)
			{
				return String.valueOf(mAssigned);
			}
			return "";
		}

		public int getBattlePoints()
		{
			return mAssigned;
		}

		public void incBattlePoints()
		{
			mAssigned++;
		}

		public void clearBattlePoints()
		{
			mAssigned = 0;
		}

		public BattleItem dup()
		{
			return new BattleItem(mType, mCount);
		}
	}

	public static List<BattleItem> getItems()
	{
		return ITEMS;
	}

	public static void clearBattlePoints()
	{
		for (BattleItem item : ITEMS)
		{
			item.clearBattlePoints();
		}
	}

	/**
	 * An array of sample (dummy) items.
	 */
	static final List<BattleItem> ITEMS = new ArrayList<>();

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

	private static void addItem(BattleItem item)
	{
		ITEMS.add(item);
	}

	private static BattleItem createItem(Strategy.Kind type, int count)
	{
		return new BattleItem(type, count);
	}

}
