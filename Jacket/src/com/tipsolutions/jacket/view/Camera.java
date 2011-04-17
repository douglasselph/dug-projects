package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;

import com.tipsolutions.jacket.math.Vector3f;

public class Camera {

	Vector3f mCameraPos = new Vector3f(0, 0, -1);
	Vector3f mLookAtPos = new Vector3f(0, 0, 0);
	Vector3f mUp = new Vector3f(0, 1, 0);
	int mWidth = 100;
	int mHeight = 100;

	public Camera() {
	}

	public void applyLookAt(GL10 gl) {
		GLU.gluLookAt(gl, 
				mCameraPos.getX(), mCameraPos.getY(), mCameraPos.getZ(), 
				mLookAtPos.getX(), mLookAtPos.getY(), mLookAtPos.getZ(), 
				mUp.getX(), mUp.getY(), mUp.getZ());
	}
	
	public void applyFrustrum(GL10 gl) {
		gl.glViewport(0, 0, mWidth, mHeight);
		float ratio = (float) mWidth / mHeight;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
	}
	
	public Vector3f getLocation() {
		return mCameraPos;
	}
	
	public Vector3f getLookAt() {
		return mLookAtPos;
	}
	
	public Vector3f getUp() {
		return mUp;
	}
	
	public Camera setLocation(Vector3f loc) {
		mCameraPos = loc;
		return this;
	}
	
	public Camera setLookAt(Vector3f loc) {
		mLookAtPos = loc;
		return this;
	}
	
	public Camera setUp(Vector3f loc) {
		mUp = loc;
		return this;
	}
	
	public Camera setScreenDimension(int width, int height) {
		mWidth = width;
		mHeight = height;
		return this;
	}

	public Vector3f getDirection() {
		Vector3f dir = new Vector3f(mLookAtPos);
		return dir.subtract(mCameraPos).normalize();
	}
}