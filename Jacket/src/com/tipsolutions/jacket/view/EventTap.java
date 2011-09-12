package com.tipsolutions.jacket.view;


public class EventTap implements IEventTap {
	
	static final short DOUBLE_TAP_TRIGGER_MS = 400;
	
	protected float mStartX;
	protected float mStartY;
 	protected long mLastTouchTime;
	protected long mStartTouchTime;
	protected ControlSurfaceView mView;
	protected Runnable mDoubleTap = null;
	
	public EventTap(ControlSurfaceView view) {
		mView = view;
	}
	
	public float getStartX() { return mStartX; }
	public float getStartY() { return mStartY; }
	
	public boolean pressDown(final float x, final float y) {
		mStartX = x;
		mStartY = y;
		mStartTouchTime = System.currentTimeMillis();
		return true;
	}
	
	public boolean pressMove(final float x, final float y) {
		return true;
	}
	
	public boolean pressUp(float x, float y){
		long curTime = System.currentTimeMillis();
		long diffTime = curTime - mStartTouchTime;
		if (mDoubleTap != null) {
			if (diffTime <= DOUBLE_TAP_TRIGGER_MS) {
				diffTime = mStartTouchTime - mLastTouchTime;
				if (diffTime <= DOUBLE_TAP_TRIGGER_MS) {
					mDoubleTap.run();
				}
			}
		}
		mLastTouchTime = curTime;
		return false;
	}
	
	public void setDoubleTap(Runnable run) {
		mDoubleTap = run;
	}
}
