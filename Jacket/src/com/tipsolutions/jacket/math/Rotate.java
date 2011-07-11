package com.tipsolutions.jacket.math;

import javax.microedition.khronos.opengles.GL10;

public class Rotate {
	protected float mYaw = 0;    // X
	protected float mPitch = 0;  // Y
	protected float mRoll = 0;   // Z
	
	public Rotate() {
	}
	
	public Rotate(float yaw, float pitch, float roll) {
		mYaw = yaw;
		mPitch = pitch;
		mRoll = roll;
	}
	
	public Rotate add(final Rotate other) {
		mYaw += other.mYaw;
		mPitch += other.mPitch;
		mRoll += other.mRoll;
		return this;
	}
	
	public void apply(GL10 gl) {
		if (getYaw() != 0) {
			gl.glRotatef(getYaw(), 1, 0, 0);
		}
		if (getPitch() != 0) {
			gl.glRotatef(getPitch(), 0, 1, 0);
		}
		if (getRoll() != 0) {
			gl.glRotatef(getRoll(), 0, 0, 1);
		}
	}
	
	public Rotate clear() {
		mYaw = 0;
		mPitch = 0;
		mRoll = 0;
		return this;
	}
	
	public float getYaw() {
		return mYaw; // X
	}
	
	public float getPitch() {
		return mPitch; // Y
	}
	
	public float getRoll() { 
		return mRoll; // Z
	}
	
	public Rotate addYaw(float yaw) { // X
		mYaw += yaw;
		return this;
	}
	
	public Rotate addPitch(float pitch) { // Y
		mPitch += pitch;
		return this;
	}
	
	public Rotate addRoll(float roll) { // Z
		mRoll += roll;
		return this;
	}
	
}
