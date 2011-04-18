package com.tipsolutions.jacket.view;

import android.util.FloatMath;

public class Controller {

	static final int MOVE_TRIGGER_MILLI = 1000;
	static final float BOUNDARY_TRIGGER_PERCENT = .15f;
	static final float SWIPE_TRIGGER_PERCENT = .40f;
	
	final IControl mControlled;
	final IView mView;
	final int mTriggerTurn;
	
	float mStartX = 0;
	float mStartY = 0;
	long mStartTime = 0;
	long mMoveStartTime = 0;
	int mTriggerRight;
	int mTriggerLeft;
	int mTriggerUpper;
	int mTriggerLower;
	boolean mMoveOkay = true;
	
	public Controller(IControl controlled, IView view) {
		mControlled = controlled;
		mView = view;
		mTriggerTurn = (int) (view.getWidth() * BOUNDARY_TRIGGER_PERCENT);
		int triggerEdge = (int) (view.getWidth() * SWIPE_TRIGGER_PERCENT);
		mTriggerRight = view.getWidth()-triggerEdge;
		mTriggerLeft = triggerEdge;
		mTriggerUpper = triggerEdge;
		mTriggerLower = view.getHeight()-triggerEdge;
	}
	
	public void pressDown(float x, float y) {
		mStartX = x;
		mStartY = y;
		mStartTime = System.currentTimeMillis();
		mMoveStartTime = mStartTime;
		mMoveOkay = true;
		mControlled.touchStart();
	}
	
	public void pressMove(float x, float y) {
		if (mMoveOkay) {
			long now = System.currentTimeMillis();
    		long timePassed = now - mMoveStartTime;
    		if (timePassed >= MOVE_TRIGGER_MILLI) {
    			mMoveStartTime = now;
    			if (x <= mTriggerLeft) {
    				mControlled.sideLeft();
    			} else if (x >= mTriggerRight) {
    				mControlled.sideRight();
    			} else if (y <= mTriggerUpper) {
    				mControlled.sideUp();
    			} else if (y >= mTriggerLower) {
    				mControlled.sideDown();
    			} else {
        			mControlled.centerLong();
    			}
    			return;
    		}
		}
		boolean reset = false;
		float diffX = x - mStartX;
		float diffY = y - mStartY;
		
		if (diffX > mTriggerTurn) {
			int times = (int) FloatMath.floor(diffX / mTriggerTurn);
			mControlled.slideRight(times);
			reset = true;
		} else if (diffX < -mTriggerTurn) {
			int times = (int) FloatMath.floor(-diffX / mTriggerTurn);
			mControlled.slideLeft(times);
			reset = true;
		} else if (diffY > mTriggerTurn) {
			int times = (int) FloatMath.floor(diffY / mTriggerTurn);
			mControlled.slideDown(times);
			reset = true;
		} else if (diffY < -mTriggerTurn) {
			int times = (int) FloatMath.floor(-diffY / mTriggerTurn);
			mControlled.slideUp(times);
			reset = true;
		}
		if (reset) {
			mMoveOkay = false;
			mStartX = x;
			mStartY = y;
		}
	}
	
	public void pressUp(float x, float y) {
		long now = System.currentTimeMillis();
		long timePassed = now - mStartTime;
		if (timePassed < MOVE_TRIGGER_MILLI) {
			mControlled.centerShort();
		}
		mControlled.touchEnd();
	}
}
