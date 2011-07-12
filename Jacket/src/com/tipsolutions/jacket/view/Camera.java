package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.util.Log;

import com.tipsolutions.jacket.data.Constants;
import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Vector3f;

public class Camera {

	Vector3f mCameraPos = new Vector3f(0, 0, -1);
	Vector3f mDirection = null;
	Vector3f mLeft = null;
	Vector3f mLookAtPos = new Vector3f(0, 0, 0);
	Vector3f mUp = new Vector3f(0, 1, 0);
	protected float mNearPlane = 1;
	protected float mFarPlane = 1000;
	protected float mAngle = 65.0f;
	protected int mHeight = 100;
	protected int mWidth = 100;

	public Camera() {
	}

	public void applyFrustrum(GL10 gl) {
//		float ratio = (float) mWidth / mHeight;
//		gl.glMatrixMode(GL10.GL_PROJECTION);
//		gl.glLoadIdentity();
//		gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
		
		gl.glViewport(0, 0, mWidth, mHeight);
		GLU.gluPerspective(gl, mAngle, (float)mWidth / mHeight, mNearPlane, mFarPlane);
	}
	
	public void applyLookAt(GL10 gl) {
		if (Constants.LOG) {
    		Log.d("Jacket", "Camera pos=" + mCameraPos.toString() + ", look=" + mLookAtPos.toString() + ", up=" + mUp.toString());
		}
		GLU.gluLookAt(gl, 
				mCameraPos.getX(), mCameraPos.getY(), mCameraPos.getZ(), 
				mLookAtPos.getX(), mLookAtPos.getY(), mLookAtPos.getZ(), 
				mUp.getX(), mUp.getY(), mUp.getZ());
	}
	
	public Vector3f getDirection() {
		if (mDirection == null) {
    		mDirection = new Vector3f(mLookAtPos);
    		mDirection.subtract(mCameraPos).normalize();
		}
		return mDirection;
	}
	
	public Vector3f getLeft() {
		if (mLeft == null) {
			mLeft = mUp.dup().cross(getDirection()).normalize();
			if (mLeft.equals(Vector3f.ZERO)) {
				if (mDirection.getX() != 0) {
					mLeft.set(new Vector3f(mDirection.getY(), -mDirection.getX(), 0));
				} else {
					mLeft.set(new Vector3f(0, mDirection.getZ(), -mDirection.getY()));
				}
			}
		}
		return mLeft;
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
	
	protected void normalize() {
		mLeft.normalize();
        mUp.normalize();
        mDirection.normalize();
	}
	
	// dx: specifies the radian angle of rotation around the up axis.
	// dy: specifies the radian angle of rotation around the left axis.
	public void rotate(float dx, float dy) {
		Matrix3f matrix = new Matrix3f();
		if (dx != 0) {
			matrix.fromAngleNormalAxis(dx, getUp());
			mLeft = matrix.apply(getLeft());
            mDirection = matrix.apply(getDirection());
            mUp = matrix.apply(getUp());
		}
		if (dy != 0) {
			matrix.fromAngleNormalAxis(dy, getLeft());
			mLeft = matrix.apply(getLeft());
            mDirection = matrix.apply(getDirection());
            mUp = matrix.apply(getUp());
	    }
		normalize();
	}

	public Camera setLocation(Vector3f loc) {
		mCameraPos = loc;
		mDirection = null;
		return this;
	}
	
	public Camera setLookAt(Vector3f loc) {
		mLookAtPos = loc;
		mDirection = null;
		return this;
	}
	
	public Camera setScreenDimension(int width, int height) {
		mWidth = width;
		mHeight = height;
		return this;
	}
	
	public Camera setUp(Vector3f loc) {
		mUp = loc;
		mLeft = null;
		return this;
	}
}