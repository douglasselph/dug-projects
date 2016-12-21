package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/19/16.
 */

public class StrategyHolder
{
	final protected Strategy	mStrategy;
	final protected int			mCount;
	boolean						mSelected;

	public StrategyHolder(Strategy type, int count)
	{
		mStrategy = type;
		mCount = count;
	}

	public StrategyHolder dup()
	{
		return new StrategyHolder(mStrategy, mCount);
	}

	public Strategy getStrategy()
	{
		return mStrategy;
	}

	public String getName(Context ctx)
	{
		return mStrategy.getName(ctx, mCount);
	}

	public String getDesc(Context ctx)
	{
		return mStrategy.getDesc(ctx, mCount);
	}

	public int getCount()
	{
		return mCount;
	}

	public boolean isSelected()
	{
		return mSelected;
	}

	public void clearSelected()
	{
		mSelected = false;
	}

	public void setSelected()
	{
		mSelected = true;
	}

	public boolean isHuman()
	{
		return mStrategy == Strategy.HUMAN;
	}
}
