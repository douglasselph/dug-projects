package com.tipsolutions.jacket.math;

public class Bounds2D {
	
	protected static final int MIN_X = 0;
	protected static final int MIN_Y = 1;
	protected static final int MAX_X = 2;
	protected static final int MAX_Y = 3;
	
	protected static final int SIZ = 4;

	protected float [] mBounds;
	
	public Bounds2D() {
		mBounds = new float[SIZ];
	}
	
	public Bounds2D(float minx, float miny, float maxx, float maxy) {
		this();
		setMinX(minx);
		setMinY(miny);
		setMaxX(maxx);
		setMaxY(maxy);
	}
	
	public float getMinX() { return mBounds[MIN_X]; }
	public float getMinY() { return mBounds[MIN_Y]; }
	public float getMaxX() { return mBounds[MAX_X]; }
	public float getMaxY() { return mBounds[MAX_Y]; }
	
	public void setMaxX(float x) { mBounds[MAX_X] = x; }
	public void setMaxY(float y) { mBounds[MAX_Y] = y; }
	public void setMinX(float x) { mBounds[MIN_X] = x; }
	public void setMinY(float y) { mBounds[MIN_Y] = y; }
	
	public float getSizeX() { return getMaxX()-getMinX(); }
	public float getSizeY() { return getMaxY()-getMinY(); }
	
	public float getMidX() { return (getMaxX()+getMinX())/2; }
	public float getMidY() { return (getMaxY()+getMinY())/2; }
	
	public boolean within(float x, float y) {
		return (x >= getMinX() && x <= getMaxX() &&
			    y >= getMinY() && y <= getMaxY());
	}
	
	public boolean isSquare() {
		return (getSizeX() == getSizeY());
	}
	
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("Bounds[");
		sbuf.append(getMinX());
		sbuf.append("->");
		sbuf.append(getMaxX());
		sbuf.append(",");
		sbuf.append(getMinY());
		sbuf.append("->");
		sbuf.append(getMaxY());
		sbuf.append("]");
		return sbuf.toString();
	}
}
