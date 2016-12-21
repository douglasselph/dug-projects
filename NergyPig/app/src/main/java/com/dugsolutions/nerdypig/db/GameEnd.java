package com.dugsolutions.nerdypig.db;

import android.content.Context;
import android.util.Log;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/19/16.
 */

public enum GameEnd
{
	END_POINTS, MAX_TURNS;

	public static GameEnd from(int val)
	{
		for (GameEnd end : values())
		{
			if (end.ordinal() == val)
			{
				return end;
			}
		}
		Log.e(MyApplication.TAG, "Bad value: " + val);
		return END_POINTS;
	}

	public String toString(Context ctx)
	{
		if (this == GameEnd.MAX_TURNS)
		{
			return ctx.getString(R.string.game_over_turns, GlobalInt.getMaxTurns());
		}
		else
		{
			return ctx.getString(R.string.game_over_points, GlobalInt.getEndPoints());
		}
	}

}
