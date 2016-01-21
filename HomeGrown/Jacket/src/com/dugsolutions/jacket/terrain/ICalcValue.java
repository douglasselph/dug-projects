package com.dugsolutions.jacket.terrain;

public interface ICalcValue
{
	void fillInfo(float x, float y, Info info);

	boolean within(float x, float y);

	void postCalc(IMapData map);
}
