package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/20/16.
 */

public enum Strategy
{
	STOP_AFTER_NUM_ROLLS(R.string.strategy_rolls), // Stop after indicated num of rolls
	STOP_AFTER_REACHED_SUM(R.string.strategy_points), // Stop after indicated sum reached
	STOP_AFTER_REACHED_EVEN(R.string.strategy_even); // Stop after reached even number count reached.

    int resId;

    Strategy(int resId)
	{
		this.resId = resId;
	}

    String getString(Context ctx)
    {
        return ctx.getString(resId);
    }
}
