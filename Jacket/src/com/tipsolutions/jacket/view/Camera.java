package com.tipsolutions.jacket.view;

import android.opengl.GLU;

import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Vector3f;

public class Camera {

	protected Vector3f mCameraPos = new Vector3f(0, 0, -1); // given
	protected Vector3f mUnitOut = null; // computed
	protected Vector3f mUnitLeft = null; // computed
	protected Vector3f mLookAtPos = new Vector3f(0, 0, 0); // given
	protected Vector3f mUp = new Vector3f(0, 1, 0); // given
	protected float mNearPlane = 1;
	protected float mFarPlane = 1000;
	protected float mAngle = 65.0f;
	protected int mHeight = 100;
	protected int mWidth = 100;
	protected float mLeft;   // left clipping plane of viewport in model space coordinates
	protected float mRight;  // right clipping plane
	protected float mTop;    // top clipping plane
	protected float mBottom; // bottom clipping plane

	public Camera() {
	}

	public void applyFrustrum(MatrixTrackingGL gl) {
		gluPerspective(gl, mAngle, (float)mWidth / mHeight, mNearPlane, mFarPlane);
	}
	
	// Doing it myself, so I can record the computed clipping planes.
	void gluPerspective(MatrixTrackingGL gl, float fovy, float aspect, float zNear, float zFar) {
		mTop = zNear * (float) Math.tan(fovy * (Math.PI / 360.0));
		mBottom = -mTop;
		mLeft = mBottom * aspect;
		mRight = mTop * aspect;
		gl.glFrustumf(mLeft, mRight, mBottom, mTop, zNear, zFar);
	}
	
	public synchronized void applyLookAt(MatrixTrackingGL gl) {
//		Log.d("DEBUG", "Eye: " + mCameraPos.toString());
//		Log.d("DEBUG", "Center: " + mLookAtPos.toString());
//		Log.d("DEBUG", "Up: " + mUp.toString());
		GLU.gluLookAt(gl, 
				mCameraPos.getX(), mCameraPos.getY(), mCameraPos.getZ(), 
				mLookAtPos.getX(), mLookAtPos.getY(), mLookAtPos.getZ(), 
				mUp.getX(), mUp.getY(), mUp.getZ());
		// Perhaps another way to rotate the camera around a point:
//		gl.glRotatef(angle, x, y, z);
	}
	
	protected Vector3f getUnitOut() {
		if (mUnitOut == null) {
    		mUnitOut = new Vector3f(mLookAtPos);
    		mUnitOut.subtract(mCameraPos).normalize();
		}
		return mUnitOut;
	}
	
	protected Vector3f getUnitLeft() {
		if (mUnitLeft == null) {
			mUnitLeft = mUp.dup().cross(getUnitOut()).normalize();
			if (mUnitLeft.equals(Vector3f.ZERO)) {
				if (mUnitOut.getX() != 0) {
					mUnitLeft.set(new Vector3f(mUnitOut.getY(), -mUnitOut.getX(), 0));
				} else {
					mUnitLeft.set(new Vector3f(0, mUnitOut.getZ(), -mUnitOut.getY()));
				}
			}
		}
		return mUnitLeft;
	}
	
	public float getDistFromTarget() {
		Vector3f curOut = new Vector3f(mLookAtPos);
		curOut.subtract(mCameraPos);
		return curOut.length();
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
	
	// Convert from a pixel location within the seen window
	// to an actual internal coordinate position.
	//
	// Note: z will always be zero.
	public Vector3f getWorldPosition(int px, int py) {
		Vector3f vec = new Vector3f();
		float wWidth = mRight - mLeft;
		float wHeight = mTop - mBottom;
		vec.setX((wWidth * (float)px/(float)mWidth) + mLeft);
		vec.setY((wHeight * (float)py/(float)mHeight) + mBottom);
		return vec;
	}
	
	// ax: specifies the radian angle of rotation around the up axis.
	// ay: specifies the radian angle of rotation around the left axis.
	//
	// Affects the lookAt and Up position defining the camera.
	protected synchronized void rotate(float ax, float ay) {
		if (ax != 0 && ay != 0) {
    		Matrix3f matrix = new Matrix3f();
    		if (ax != 0) {
    			matrix.fromAngleNormalAxis(ax, getUp());
    			mUnitLeft = matrix.apply(getUnitLeft()).normalize();
                mUnitOut = matrix.apply(getUnitOut()).normalize();
                mUp = matrix.apply(getUp()).normalize();
    		}
    		if (ay != 0) {
    			matrix.fromAngleNormalAxis(ay, getUnitLeft());
    			mUnitLeft = matrix.apply(getUnitLeft()).normalize();
                mUnitOut = matrix.apply(getUnitOut()).normalize();
                mUp = matrix.apply(getUp()).normalize();
    	    }
    		float curOutDist = getDistFromTarget();
    		mLookAtPos = new Vector3f(mUnitOut).multiply(curOutDist).add(mCameraPos);
		}
	}
	
	// Adjust what we are looking at by the indicated amount.
	protected void lookAtAdjust(float dx_px, float dy_px) {
		// Convert to model space.
		float dx = dx_px / mWidth * (mRight - mLeft);
		float dy = dy_px / mHeight * (mTop - mBottom);
		float curOutDist = getDistFromTarget();
		float ax = 0;
		float ay = 0;
		if (dx != 0) {
    		ax = (float) Math.atan(dx / curOutDist) * -1;
		}
		if (dy != 0) {
    		ay = (float) Math.atan(dy / curOutDist);
		}
		rotate(ax, ay);
	}

	public synchronized Camera setLocation(Vector3f loc) {
		mCameraPos = loc;
		mUnitOut = null;
		mUnitLeft = null;
		return this;
	}
	
	public synchronized Camera setLookAt(Vector3f loc) {
		mLookAtPos = loc;
		mUnitOut = null;
		mUnitLeft = null;
		return this;
	}
	
	public Camera setScreenDimension(int width, int height) {
		mWidth = width;
		mHeight = height;
		return this;
	}
	
	public synchronized Camera setUp(Vector3f loc) {
		mUp = loc;
		mUnitLeft = null;
		mUnitOut = null;
		return this;
	}
}