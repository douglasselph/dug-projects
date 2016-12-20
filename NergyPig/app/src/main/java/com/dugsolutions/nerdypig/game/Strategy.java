package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/19/16.
 */

public class Strategy
{
	public enum Kind
	{
		STOP_AFTER_NUM_ROLLS, // Stop after indicated num of rolls
		STOP_AFTER_REACHED_SUM, // Stop after indicated sum reached
		STOP_AFTER_REACHED_EVEN, // Stop after reached even number count reached.
	}

	final protected Kind	mType;
	final protected int		mCount;

	public Strategy(Kind type, int count)
	{
		mType = type;
		mCount = count;
	}

	public String toString(Context ctx)
	{
		StringBuffer sbuf = new StringBuffer();

		if (mType == Kind.STOP_AFTER_NUM_ROLLS)
		{
			sbuf.append(ctx.getString(R.string.strategy_rolls, mCount));
		}
		else if (mType == Kind.STOP_AFTER_REACHED_SUM)
		{
			sbuf.append(ctx.getString(R.string.strategy_points, mCount));
		}
		else if (mType == Kind.STOP_AFTER_REACHED_EVEN)
		{
			sbuf.append(ctx.getString(R.string.strategy_even, mCount));
		}
		else
		{
			sbuf.append(ctx.getString(R.string.unknown));

		}
		return sbuf.toString();
	}

	public Strategy dup()
	{
		return new Strategy(mType, mCount);
	}

	public Kind getKind()
	{
		return mType;
	}

	public int getCount()
	{
		return mCount;
	}

}
