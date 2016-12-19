package com.dugsolutions.nerdypig.db;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class BattleLine
{
	public enum BattleType
	{
		STOP_AFTER_NUM_ROLLS, // Stop after indicated num of rolls
		STOP_AFTER_REACHED_SUM, // Stop after indicated sum reached
		STOP_AFTER_REACHED_EVEN, // Stop after reached even number count reached.
	}

	/**
	 * A dummy item representing a piece of content.
	 */
	public static class BattleItem
	{
		final BattleType	mType;
		final int			mCount;
		short				mAssigned;	// How many battle points assigned to this strategy

		public BattleItem(BattleType type, int count)
		{
			mType = type;
			mCount = count;
		}

		public String getId()
		{
			if (mAssigned > 0)
			{
				return String.valueOf(mAssigned);
			}
			return "";
		}

		public String toString(Context ctx)
		{
			StringBuffer sbuf = new StringBuffer();

			if (mType == BattleType.STOP_AFTER_NUM_ROLLS)
			{
				sbuf.append(ctx.getString(R.string.strategy_rolls, mCount));
			}
			else if (mType == BattleType.STOP_AFTER_REACHED_SUM)
			{
				sbuf.append(ctx.getString(R.string.strategy_points, mCount));
			}
			else if (mType == BattleType.STOP_AFTER_REACHED_EVEN)
			{
				sbuf.append(ctx.getString(R.string.strategy_even, mCount));
			}
			else
			{
				sbuf.append(ctx.getString(R.string.unknown));

			}
			return sbuf.toString();
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
		for (int rolls = 3; rolls <= 6; rolls++)
		{
			addItem(createItem(BattleType.STOP_AFTER_NUM_ROLLS, rolls));
		}
		for (int sum = 20; sum < 30; sum++)
		{
			addItem(createItem(BattleType.STOP_AFTER_REACHED_SUM, sum));
		}
		addItem(createItem(BattleType.STOP_AFTER_REACHED_EVEN, 2));
	}

	private static void addItem(BattleItem item)
	{
		ITEMS.add(item);
	}

	private static BattleItem createItem(BattleType type, int count)
	{
		return new BattleItem(type, count);
	}

}
