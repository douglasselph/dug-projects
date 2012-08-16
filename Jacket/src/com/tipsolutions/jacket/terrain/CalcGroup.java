package com.tipsolutions.jacket.terrain;

import java.util.ArrayList;

import com.tipsolutions.jacket.math.Vector3f;

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
		Vector3f normal = new Vector3f();
		float height = 0;

		for (ICalcValue calc : mList)
		{
			if (calc.within(x, y))
			{
				Info info = calc.getInfo(x, y);
				if (info != null)
				{
					height += info.getHeight();
					normal.add(info.getNormal());
					normal.normalize();
				}
			}
		}
		return new Info(height, normal);
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
