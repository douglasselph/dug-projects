package com.dugsolutions.fell.map.gen2;

/**
 * Generate a constant height over all values. Can be sub-classed for generators
 * that have at least a bounds and a height.
 */
public class GenZConstant extends GenZBase {
	protected float mHeight;

	public GenZConstant(float height) {
		super();
		mHeight = height;
	}
	
	public GenZConstant(int count, float height) {
		super(count);
		mHeight = height;
	}

	public float getHeight() {
		return mHeight;
	}

	@Override
	protected void setZ(int x, int y) {		
		icalc.addHeight(x, y, mHeight);
	}

}
