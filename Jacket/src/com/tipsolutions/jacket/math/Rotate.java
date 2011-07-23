package com.tipsolutions.jacket.math;

import javax.microedition.khronos.opengles.GL10;

public class Rotate {
	protected float mPitch = 0;  // Y radians
	protected float mRoll = 0;   // Z radians
	protected float mYaw = 0;    // X radians
	
	public Rotate() {
	}
	
	public Rotate(float yaw, float pitch, float roll) {
		mYaw = yaw;     // X radians
		mPitch = pitch; // Y radians
		mRoll = roll;   // Z radians
	}
	
	public Rotate(double yaw, double pitch, double roll) {
		mYaw = (float)yaw;     // X radians
		mPitch = (float)pitch; // Y radians
		mRoll = (float)roll;   // Z radians
	}
	
	public Rotate(float yaw, float pitch, float roll, boolean degrees) {
		if (degrees) {
    		mYaw = (float) Math.toRadians(yaw);
    		mPitch = (float) Math.toRadians(pitch);
    		mRoll = (float) Math.toRadians(roll);
		} else {
    		mYaw = yaw;
    		mPitch = pitch;
    		mRoll = roll;
		}
	}
	
	public Rotate add(final Rotate other) {
		mYaw += other.mYaw;
		mPitch += other.mPitch;
		mRoll += other.mRoll;
		return this;
	}
	
	public void clamp() {
		mYaw = MathUtils.clamp(mYaw);
		mPitch = MathUtils.clamp(mPitch);
		mRoll = MathUtils.clamp(mRoll);
	}
	
	public Rotate addPitch(float pitch) { // Y radians
		mPitch += pitch;
		return this;
	}
	
	public Rotate addRoll(float roll) { // Z radians
		mRoll += roll;
		return this;
	}
	
	public Rotate addYaw(float yaw) { // X radians
		mYaw += yaw;
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
	
	public float getAngleX() {
		return mYaw; 
	}
	
	public void setAngleX(float x) {
		mYaw = x;
	}
	
	public void setAngleX(double x) {
		mYaw = (float)x;
	}
	
	public float getAngleXDegrees() {
		return (float) Math.toDegrees(mYaw); 
	}
	
	public float getAngleY() {
		return mPitch; 
	}
	
	public void setAngleY(float y) {
		mPitch = y;
	}
	
	public void setAngleY(double y) {
		mPitch = (float) y;
	}
	
	public float getAngleYDegrees() {
		return (float) Math.toDegrees(mPitch); 
	}
	
	public float getAngleZ() { 
		return mRoll; 
	}
	
	public void setAngleZ(float z) {
		mRoll = z;
	}
	
	public void setAngleZ(double z) {
		mRoll = (float)z;
	}
	
	public float getAngleZDegrees() { 
		return (float) Math.toDegrees(mRoll); 
	}
	
	public float getPitch() {
		return mPitch; // Y
	}
	
	public float getRoll() { 
		return mRoll; // Z
	}
	
	public float getYaw() {
		return mYaw; // X
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
//		sbuf.append(" R=");
//		sbuf.append(getAngleX());
//		sbuf.append(",");
//		sbuf.append(getAngleY());
//		sbuf.append(",");
//		sbuf.append(getAngleZ());
		sbuf.append("X=");
		sbuf.append(getAngleXDegrees());
		sbuf.append(",Y=");
		sbuf.append(getAngleYDegrees());
		sbuf.append(",Z=");
		sbuf.append(getAngleZDegrees());
		return sbuf.toString();
	}
	
}
