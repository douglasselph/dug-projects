package com.tipsolutions.jacket.view;

public class TwirlEventTap implements IEventTap {

	static final short DOUBLE_TAP_TRIGGER_MS = 400;
	
	public interface Rotate {
		void rotate(double xAngle, double yAngle);
	};
	
	float _x;
	float _y;
 	long mLastTouchTime;
	long mStartTouchTime;
	ControlSurfaceView mView;
	Runnable mDoubleTap = null;
	Rotate mRotate;
	
	public TwirlEventTap(ControlSurfaceView view, Rotate rotate) {
		mView = view;
		mRotate = rotate;
	}
	
	public boolean pressDown(final float x, final float y) {
		_x = x;
		_y = y;
		mStartTouchTime = System.currentTimeMillis();
		return true;
	}
	
	public boolean pressMove(final float x, final float y) {
		mView.queueEvent(new Runnable() {
			public void run() {
				float xdiff = (_x - x);
				float ydiff = (_y - y);
				double yAngle = Math.toRadians(xdiff);
				double xAngle = Math.toRadians(ydiff);
				mRotate.rotate(xAngle, yAngle);
				mView.requestRender();
				_x = x;
				_y = y;
			}
		});
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
