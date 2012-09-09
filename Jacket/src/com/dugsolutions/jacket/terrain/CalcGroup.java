package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;

/**
 * Generate the sum of a list of generators.
 */
public class CalcGroup implements ICalcValue
{
	protected ArrayList<ICalcValue>	mList	= new ArrayList<ICalcValue>();

	public CalcGroup add(ICalcValue item)
	{
		mList.add(item);
		return this;
	}

	public void fillInfo(float x, float y, Info info)
	{
		for (ICalcValue calc : mList)
		{
			calc.fillInfo(x, y, info);
		}
	}

	public boolean within(float x, float y)
	{
		for (ICalcValue calc : mList)
		{
			if (calc.within(x, y))
			{
				return true;
			}
		}
		return false;
	}

}
