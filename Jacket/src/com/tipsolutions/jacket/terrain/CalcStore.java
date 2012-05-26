package com.tipsolutions.jacket.terrain;

import java.util.HashMap;

import com.tipsolutions.jacket.math.Vector3f;

/**
 * Takes any calculator, and if the value generated has been generated already
 * then the value is taken from the stored hash map.
 */
public class CalcStore implements ICalcValue {

	ICalcValue mValue;
	HashMap<Float,HashMap<Float,Info>> mHeightMap = new HashMap<Float,HashMap<Float,Info>>();
	
	public CalcStore(ICalcValue calc) {
		mValue = calc;
	}
	
	public Info getInfo(float x, float y) {
		Info info = mapQuery(x, y);
		if (info != null) {
			return info;
		}
		info = mValue.getInfo(x, y);
		mapStore(x, y, info);
		return info;
	}

	public boolean within(float x, float y) {
		return mValue.within(x, y);
	}
	
	Info mapQuery(float x, float y) {
		HashMap<Float,Info> xMap;
		if (mHeightMap.containsKey(x)) {
			xMap = mHeightMap.get(x);
			if (xMap.containsKey(y)) {
				return xMap.get(y);
			}
		}
		return null;
	}
	
	void mapStore(float x, float y, Info value) {
		HashMap<Float,Info> xMap;
		if (mHeightMap.containsKey(x)) {
			xMap = mHeightMap.get(x);
		} else {
			xMap = new HashMap<Float,Info>();
			mHeightMap.put(x, xMap);
		}
		xMap.put(y, value);
	}

}
