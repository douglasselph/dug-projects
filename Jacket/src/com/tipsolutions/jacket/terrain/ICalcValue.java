package com.tipsolutions.jacket.terrain;

public interface ICalcValue {
	Info getInfo(float x, float y);
	boolean within(float x, float y);
}
