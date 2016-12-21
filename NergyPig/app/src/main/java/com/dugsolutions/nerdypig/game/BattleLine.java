package com.dugsolutions.nerdypig.game;

import com.dugsolutions.nerdypig.game.Strategy;
import com.dugsolutions.nerdypig.game.StrategyHolder;

import java.util.ArrayList;
import java.util.List;

public class BattleLine
{

	static final List<StrategyHolder> ITEMS = new ArrayList<>();

	static
	{
		addItem(createItem(Strategy.HUMAN, 0));

		for (int rolls = 3; rolls <= 7; rolls++)
		{
			addItem(createItem(Strategy.STOP_AFTER_NUM_ROLLS, rolls));
		}
		for (int sum = 20; sum < 30; sum++)
		{
			addItem(createItem(Strategy.STOP_AFTER_REACHED_SUM, sum));
		}
		addItem(createItem(Strategy.STOP_AFTER_REACHED_EVEN, 2));
	}

	static void addItem(StrategyHolder item)
	{
		ITEMS.add(item);
	}

	static StrategyHolder createItem(Strategy type, int count)
	{
		return new StrategyHolder(type, count);
	}

	public static List<StrategyHolder> getItems()
	{
		return ITEMS;
	}

	public static StrategyHolder getSelectedStrategy()
	{
		for (StrategyHolder strategy : ITEMS)
		{
			if (strategy.isSelected())
			{
				return strategy;
			}
		}
		return null;
	}

	public static void clearSelected()
	{
		for (StrategyHolder strategy : ITEMS)
		{
			strategy.clearSelected();
		}
	}

}