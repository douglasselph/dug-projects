package com.dugsolutions.nerdypig.game;

import android.content.Context;

import com.dugsolutions.nerdypig.R;

import java.util.ArrayList;

/**
 * Created by dug on 12/19/16.
 */

public class Player
{
	Strategy			mStrategy;
	String				mDesc;
	int					mWins;
	ArrayList<Integer>	mValues;

	public Player(Strategy strategy, String desc)
	{
		mStrategy = strategy;
		mDesc = desc;
		mValues = new ArrayList<>();
	}

	public Strategy getStrategy()
	{
		return mStrategy;
	}

	public void reset()
	{
		mWins = 0;
		mValues = new ArrayList<>();
	}

	public void win()
	{
		mWins++;
	}

	public void addValue(int value)
	{
		mValues.add(value);
	}

	public String getDescGamesWon(int numGames)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(mDesc);
		sbuf.append(", WINS=");

		double percent = (double) mWins / (double) numGames;

		sbuf.append(String.format("%.2g", percent));
		sbuf.append("%%");
		sbuf.append(" [");
		sbuf.append(mWins);
		sbuf.append("]");
		return sbuf.toString();
	}

	public String getDescAverageTurns(Context ctx)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(mDesc);
		sbuf.append(". ");
		if (mValues.size() == 0)
		{
			sbuf.append(ctx.getString(R.string.error_turns));
		}
		else
		{
			sbuf.append(ctx.getString(R.string.report_turns, getValueAverage()));
		}
		return sbuf.toString();
	}

	public String getDescAverageScore(Context ctx)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(mDesc);
		sbuf.append(". ");
		if (mValues.size() == 0)
		{
			sbuf.append(ctx.getString(R.string.error_score));
		}
		else
		{
			sbuf.append(ctx.getString(R.string.report_score, getValueAverage()));
		}
		return sbuf.toString();
	}

	double getValueAverage()
	{
		int total = 0;
		for (Integer i : mValues)
		{
			total += i;
		}
		return (double) total / (double) mValues.size();
	}
}
