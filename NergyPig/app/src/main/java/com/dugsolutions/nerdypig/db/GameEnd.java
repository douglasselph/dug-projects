package com.dugsolutions.nerdypig.db;

import android.util.Log;

import com.dugsolutions.nerdypig.MyApplication;

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

}
