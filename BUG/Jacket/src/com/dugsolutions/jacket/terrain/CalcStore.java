package com.dugsolutions.jacket.terrain;

/**
 * Takes any calculator, and if the value generated has been generated already
 * then the value is taken from the stored hash map.
 */
public class CalcStore extends FloatMap<Info> implements ICalcValue
{
	protected ICalcValue	mValue;

	public CalcStore(ICalcValue calc)
	{
		mValue = calc;
	}

	public void fillInfo(float x, float y, Info info)
	{
		Info cur = get(x, y);
		if (cur != null)
		{
			info.set(cur.dup());
		}
		else
		{
			mValue.fillInfo(x, y, info);
			put(x, y, info.dup());
		}
	}

	public boolean within(float x, float y)
	{
		return mValue.within(x, y);
	}

	@Override
	public void postCalc(IMapData query)
	{
	}
}
