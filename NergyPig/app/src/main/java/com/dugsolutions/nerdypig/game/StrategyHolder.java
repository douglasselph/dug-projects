package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/19/16.
 */

public class StrategyHolder
{
	final protected Strategy	mType;
	final protected int			mCount;

	public StrategyHolder(Strategy type, int count)
	{
		mType = type;
		mCount = count;
	}

	public String toString(Context ctx)
	{
		return mType.getString(ctx);
	}

	public StrategyHolder dup()
	{
		return new StrategyHolder(mType, mCount);
	}

	public Strategy getKind()
	{
		return mType;
	}

	public int getCount()
	{
		return mCount;
	}

}
