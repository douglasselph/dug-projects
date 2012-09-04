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

	public Info getInfo(float x, float y)
	{
		Info result = null;

		for (ICalcValue calc : mList)
		{
			if (calc.within(x, y))
			{
				if (result == null)
				{
					result = new Info();
				}
				Info info = calc.getInfo(x, y);
				if (info != null)
				{
					result.add(info);
				}
			}
		}
		return result;
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
