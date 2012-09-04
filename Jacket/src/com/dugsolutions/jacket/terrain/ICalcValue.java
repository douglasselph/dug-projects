package com.dugsolutions.jacket.terrain;

public interface ICalcValue {
	Info getInfo(float x, float y);
	boolean within(float x, float y);
}
