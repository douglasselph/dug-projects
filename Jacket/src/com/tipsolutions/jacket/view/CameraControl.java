package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Vector3f;

public class CameraControl extends Camera implements IControl {

	float mAnglePerTouch = (float) Math.PI/20;
	boolean mBackwards = false;
	boolean mChanged = true;
	float mDistPerTouch = 1;
	
	@Override
	public void centerLong() {
		Vector3f amt = getDirection().dup().multiply(mDistPerTouch);
		if (mBackwards) {
    		getLocation().subtract(amt);
		} else {
    		getLocation().add(amt);
		}
		mChanged = true;
	}
	
	@Override
	public void centerShort() {
		mBackwards = !mBackwards;
	}

	public boolean isBackwards() {
		return mBackwards;
	}

	public void onDraw(GL10 gl) {
		if (mChanged) {
			applyLookAt(gl);
			mChanged = false;
		}
	}

	@Override
	public void sideDown() {
		Vector3f amt = getUp().dup().multiply(mDistPerTouch);
		getLocation().subtract(amt);
		mChanged = true;
	}

	@Override
	public void sideLeft() {
		Vector3f amt = getLeft().dup().multiply(mDistPerTouch);
		getLocation().add(amt);
		mChanged = true;
	}

	@Override
	public void sideRight() {
		Vector3f amt = getLeft().dup().multiply(mDistPerTouch);
		getLocation().subtract(amt);
		mChanged = true;
	}

	@Override
	public void sideUp() {
		Vector3f amt = getUp().dup().multiply(mDistPerTouch);
		getLocation().add(amt);
		mChanged = true;
	}

	@Override
	public void slideDown(int times) {
		rotate(0, mAnglePerTouch*times);
		mChanged = true;
	}

	@Override
	public void slideLeft(int times) {
		rotate(mAnglePerTouch*times, 0);
		mChanged = true;
	}

	@Override
	public void slideRight(int times) {
		rotate(-mAnglePerTouch*times, 0);
		mChanged = true;
	}
	
	@Override
	public void slideUp(int times) {
		rotate(0, -mAnglePerTouch*times);
		mChanged = true;
	}

	@Override
	public void touchEnd() {
	}

	@Override
	public void touchStart() {
	}

	@Override
	public Camera setLocation(Vector3f loc) {
		mChanged = true;
		return super.setLocation(loc);
	}

	@Override
	public Camera setLookAt(Vector3f loc) {
		mChanged = true;
		return super.setLookAt(loc);
	}

	@Override
	public Camera setScreenDimension(int width, int height) {
		mChanged = true;
		return super.setScreenDimension(width, height);
	}

	@Override
	public Camera setUp(Vector3f loc) {
		mChanged = true;
		return super.setUp(loc);
	}
	
}
