package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Vector3f;

/**
 * Generate a constant height over all values.
 * Can be sub-classed for generators that have at least a bounds and a height.
 */
public class CalcConstant extends CalcValue {

	protected float mHeight;
	
	public CalcConstant(float height) {
		mHeight = height;
	}
	
	public CalcConstant(float height, Bounds2D bounds) {
		super(bounds);
		mHeight = height;
	}
	
	public float getHeight() {
		return mHeight;
	}
	
	public Info getInfo(float x, float y) {
		if (within(x, y)) {
			return new Info(mHeight, new Vector3f(0, 0, 1));
		}
		return null;
	}

}
