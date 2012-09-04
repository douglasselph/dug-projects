package com.dugsolutions.jacket.math;

public class Point {
	float mTime;
	float mValue;
		
	public Point() {
		mTime = 0;
		mValue = 0;
	}
	
	public Point(float time, float value) {
		mTime = time;
		mValue = value;
	}

	public float getTime() { return mTime; }
	public float getValue() { return mValue; }
	public void setTime(float t) { mTime = t; }
	public void setValue(float v) { mValue = v; }
}
