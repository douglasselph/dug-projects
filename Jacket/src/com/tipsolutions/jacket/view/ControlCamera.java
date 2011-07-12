package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Vector3f;

public class ControlCamera extends Camera implements IEventTap {

	static final int DRAG_TRIGGER_PX = 10;
	static final int FORWARD_TRIGGER_MS = 400;
	static final int FORWARD_INTERVAL_MS = 100;
	static final int BACKWARDS_RESET_MS = 2000;
	
	boolean mBackwards = false;
	boolean mDidMove = false;
	boolean mForwardOn = false;
	float mDistForwardPerTimeFrame = 0.1f;
	long mStartTouchTime;
	long mBackwardsTouchTime;
	int mStartX;
	int mStartY;
	Runnable mDoubleTap = null;
	
	public boolean isBackwards() {
		return mBackwards;
	}

	public void moveOut() {
		Vector3f amt = getUnitOut().dup().multiply(mDistForwardPerTimeFrame);
		
		if (mBackwards) {
    		mCameraPos.subtract(amt);
    		mLookAtPos.subtract(amt);
		} else {
    		mCameraPos.add(amt);
    		mLookAtPos.add(amt);
		}
	}

	@Override
	public boolean pressDown(float x, float y) {
		long curTime = System.currentTimeMillis();
		mStartTouchTime = curTime;
		mStartX = (int) x;
		mStartY = (int) y;
		mDidMove = false;
		mForwardOn = false;
		
		long diffTime = curTime - mBackwardsTouchTime;
		if (diffTime >= BACKWARDS_RESET_MS) {
			mBackwards = false;
		}
		return true;
	}

	@Override
	public boolean pressMove(float x, float y) {
		long curTime = System.currentTimeMillis();
		float diffx = x - mStartX;
		float diffy = y - mStartY;
		
		if (Math.abs(diffx) >= DRAG_TRIGGER_PX || Math.abs(diffy) >= DRAG_TRIGGER_PX) {
			lookAtAdjust(diffx*2, diffy*2);
			mStartX = (int) x;
			mStartY = (int) y;
			mDidMove = true;
			mForwardOn = false;
			mStartTouchTime = curTime;
		} else {
			long diffTime = curTime - mStartTouchTime;
			boolean moveOut = false;

			if (mForwardOn) {
				if (diffTime >= FORWARD_INTERVAL_MS) {
					moveOut = true;
				}
			} else if (diffTime >= FORWARD_TRIGGER_MS) {
				moveOut = true;
			}
			if (moveOut) {
				moveOut();
				mForwardOn = true;
				mStartTouchTime = curTime;
				mDidMove = true;
			}
		}
		return true;
	}
	
	@Override
	public boolean pressUp(float x, float y) {
		if (!mDidMove) {
			long curTime = System.currentTimeMillis();
    		long diffTime = curTime - mStartTouchTime;
    		if (diffTime <= FORWARD_TRIGGER_MS) {
    			if (mDoubleTap != null) {
        			diffTime = mStartTouchTime - mBackwardsTouchTime;
        			if (diffTime <= FORWARD_TRIGGER_MS) {
        				mDoubleTap.run();
        			}
    			}
    			mBackwards = true;
    			mBackwardsTouchTime = curTime;
    		}
		}
		return true;
	}

	@Override
	public void applyFrustrum(GL10 gl) {
		super.applyFrustrum(gl);
		mDistForwardPerTimeFrame = (mRight - mLeft) / 10f;
	}
	
	public void setDoubleTap(Runnable run) {
		mDoubleTap = run;
	}

//	public void sideDown() {
//		Vector3f amt = getUp().dup().multiply(mDistPerTouch);
//		getLocation().subtract(amt);
//	}
//
//	public void sideLeft() {
//		Vector3f amt = getUnitLeft().dup().multiply(mDistPerTouch);
//		getLocation().add(amt);
//	}
//
//	public void sideRight() {
//		Vector3f amt = getUnitLeft().dup().multiply(mDistPerTouch);
//		getLocation().subtract(amt);
//	}
//
//	public void sideUp() {
//		Vector3f amt = getUp().dup().multiply(mDistPerTouch);
//		getLocation().add(amt);
//	}
}
