package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class FloatMap<DATA>
{
	protected HashMap<Float, HashMap<Float, DATA>>	mMap	= new HashMap<Float, HashMap<Float, DATA>>();

	public FloatMap()
	{
	}

	public DATA get(float x, float y)
	{
		HashMap<Float, DATA> xMap;
		if (mMap.containsKey(x))
		{
			xMap = mMap.get(x);
			if (xMap.containsKey(y))
			{
				return xMap.get(y);
			}
		}
		return null;
	}

	public Collection<DATA> getValuesOnX(float x)
	{
		if (mMap.containsKey(x))
		{
			return mMap.get(x).values();
		}
		return null;
	}

	public ArrayList<DATA> getValuesOnY(float y)
	{
		ArrayList<DATA> list = new ArrayList<DATA>();

		for (HashMap<Float, DATA> xMap : mMap.values())
		{
			if (xMap.containsKey(y))
			{
				list.add(xMap.get(y));
			}
		}
		return list;
	}

	public HashMap<Float, DATA> getX(float x)
	{
		return mMap.get(x);
	}

	public void put(float x, float y, DATA value)
	{
		HashMap<Float, DATA> xMap;
		if (mMap.containsKey(x))
		{
			xMap = mMap.get(x);
		}
		else
		{
			xMap = new HashMap<Float, DATA>();
			mMap.put(x, xMap);
		}
		xMap.put(y, value);
	}
}
