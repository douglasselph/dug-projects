package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/20/16.
 */

public enum Strategy
{
	STOP_AFTER_NUM_ROLLS(R.string.strategy_rolls_name, R.string.strategy_rolls_desc),
	STOP_AFTER_REACHED_SUM(R.string.strategy_points_name, R.string.strategy_points_desc),
	STOP_AFTER_REACHED_EVEN(R.string.strategy_even_name, R.string.strategy_even_desc);

    int mResName;
    int mResDesc;

    Strategy(int name, int desc)
	{
		mResName = name;
        mResDesc = desc;
	}

    String getName(Context ctx, int value)
    {
        return ctx.getString(mResName, value);
    }

    String getDesc(Context ctx, int value)
    {
        return ctx.getString(mResDesc, value);
    }
}
