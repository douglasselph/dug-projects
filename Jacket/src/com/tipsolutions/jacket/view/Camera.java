package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.Matrix;

import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.Vector4f;

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
	protected boolean mDoOrtho = false;
	
	public float getWidth() { return mWidth; }
	public float getHeight() { return mHeight; }

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
		if (mDoOrtho) {
			gl.glOrthof(mLeft, mRight, mBottom, mTop, zNear, zFar);
		} else {
			gl.glFrustumf(mLeft, mRight, mBottom, mTop, zNear, zFar);
		}
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
	
	public class Project {
		
		float [] mModelview = new float[16];
		float [] mProjection = new float[16];
		
		public Project(MatrixTrackingGL gl) {
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.getMatrix(mModelview, 0);
			
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.getMatrix(mProjection, 0);
		}
		
		// Given a world coordinate, return the screen based coordinate
		// this would project onto.
		//
		// Note: this routine original created for testing/debugging purposes.
		public Vector4f project(Vector3f worldVec) {
			float [] wVec = new float[4];

			wVec[0] = worldVec.getX();
			wVec[1] = worldVec.getY();
			wVec[2] = worldVec.getZ();
			wVec[3] = 1;

			float [] eyeVec = new float[4];
			Matrix.multiplyMV(eyeVec, 0, mModelview, 0, wVec, 0);

			float [] clipVec = new float[4];
			Matrix.multiplyMV(clipVec, 0, mProjection, 0, eyeVec, 0);

			float [] normDevCoords = new float[4];
			float div = 1f/clipVec[3];
			normDevCoords[0] = clipVec[0] * div;
			normDevCoords[1] = clipVec[1] * div;
			normDevCoords[2] = clipVec[2] * div;
			normDevCoords[3] = 1f;

			float [] winCoords = new float[3];
			winCoords[0] = (normDevCoords[0] *.5f + .5f) * mWidth;
			winCoords[1] = (normDevCoords[1] *.5f + .5f) * mHeight;
			winCoords[2] = (1 + normDevCoords[2]) * .5f;

			return new Vector4f(winCoords[0], winCoords[1], winCoords[2], clipVec[3]);
		}
		
		// Unproject from a screen pixel coordinate to a world coordinate.
		// Input: px, py - the screen coordinates
		//	      clipW - used to determine the final z coordinate (must be >= 1)
		// Returns: 3 coordinate pair for world vec position.
		//	        The w coordinate is returned as well. Might be useful.
		public Vector4f unproject(float px, float py, float clipW) {
			float [] winCoordsR = new float[3];

			winCoordsR[0] = px;
			winCoordsR[1] = py;
			winCoordsR[2] = 0;

			float [] normDevCoordsR = new float[4];
			normDevCoordsR[0] = ((winCoordsR[0] / mWidth) - .5f)/.5f;
			normDevCoordsR[1] = ((winCoordsR[1]/ mHeight) - .5f)/.5f;
			normDevCoordsR[2] = ((winCoordsR[2] / .5f - 1));
			normDevCoordsR[3] = 1f;

			float [] clipVecR = new float[4];
			clipVecR[0] = normDevCoordsR[0] * clipW;
			clipVecR[1] = normDevCoordsR[1] * clipW;
			clipVecR[2] = normDevCoordsR[2] * clipW;
			clipVecR[3] = clipW;

			float [] scratch = new float[16];
			Matrix.multiplyMM(scratch, 0, mProjection, 0, mModelview, 0);
			Matrix.invertM(scratch, 0, scratch, 0);

			float [] worldVecR = new float[4];
			Matrix.multiplyMV(worldVecR, 0, scratch, 0, clipVecR, 0);

			return new Vector4f(worldVecR);
		}
		
		public class Slope {
			float mSlopeXY;
			float mSlopeXZ;
			boolean mYzero;
			boolean mZzero;
			
			public Slope(Vector3f pt1, Vector3f pt2) {
				float dX = pt2.getX() - pt1.getX();
				float dY = pt2.getY() - pt1.getY();
				float dZ = pt2.getZ() - pt1.getZ();
				
				if (dY == 0) {
					mSlopeXY = 0;
					mYzero = true;
				} else {
    				mSlopeXY = dX/dY;
    				mYzero = false;
				}
				if (dZ == 0) {
					mSlopeXZ = 0;
    				mZzero = true;
				} else {
    				mSlopeXZ = dX/dZ;
    				mZzero = false;
				}
			}
			
			public boolean equals(Slope slope) {
				return mSlopeXY == slope.mSlopeXY && 
					   mSlopeXZ == slope.mSlopeXZ && 
					   mYzero == slope.mYzero &&
					   mZzero == slope.mZzero;
			}
			
			public boolean equals(Slope slope, float within) {
				if (within == 0) {
					return equals(slope);
				}
				if (mYzero != slope.mYzero || mZzero != slope.mZzero) {
					return false;
				}
				float xyDiff = Math.abs(mSlopeXY - slope.mSlopeXY);
				float xzDiff = Math.abs(mSlopeXZ - slope.mSlopeXZ);
				
				return (xyDiff <= within && xzDiff <= within);
			}
		};
		
		public class Line {
			Vector4f mClip1;
			Vector4f mClip2;
			Slope mSlope;
			
			public Line(float px, float py) {
				mClip1 = unproject(px, py, 1f);
				mClip2 = unproject(px, py, 2f);
				mSlope = new Slope(mClip1, mClip2);
			}
			
			public boolean intersects(Vector3f pt, float within) {
				Slope slope = new Slope(pt, mClip2);
				return mSlope.equals(slope, within);
			}
		};
		
		public Line unproject(float px, float py) {
			return new Line(px, py);
		}
	};
	
	
	// Convert from a pixel location within the seen window
	// to an actual internal coordinate position.
//	public Vector3f getWorldPosition(MatrixTrackingGL gl, float px, float py, float pz) {
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		float [] modelview = new float[16];
//		gl.getMatrix(modelview, 0);
//		
//		gl.glMatrixMode(GL10.GL_PROJECTION);
//		float [] projection = new float[16];
//		gl.getMatrix(projection, 0);
//		
//		float clipW = pz;
//		float [] winCoordsR = new float[3];
//		float z = pz;
//		winCoordsR[0] = px;
//		winCoordsR[1] = py;
//		winCoordsR[2] = 1f - 1f/z + 1f/(z*1000) + 1f/(z*1000000);
//		
//		float [] normDevCoordsR = new float[4];
//		normDevCoordsR[0] = ((winCoordsR[0] / mWidth) - .5f)/.5f;
//		normDevCoordsR[1] = ((winCoordsR[1] / mHeight) - .5f)/.5f;
//		normDevCoordsR[2] = ((winCoordsR[2] / .5f - 1));
//		normDevCoordsR[3] = 1f;
//
//		float [] clipVecR = new float[4];
//		clipVecR[0] = normDevCoordsR[0] * clipW;
//		clipVecR[1] = normDevCoordsR[1] * clipW;
//		clipVecR[2] = normDevCoordsR[2] * clipW;
//		clipVecR[3] = z;
//		
//		float [] scratch = new float[16];
//		Matrix.multiplyMM(scratch, 0, projection, 0, modelview, 0);
//		Matrix.invertM(scratch, 0, scratch, 0);
//
//		float [] worldVecR = new float[4];
//		Matrix.multiplyMV(worldVecR, 0, scratch, 0, clipVecR, 0);
//
//		return new Vector3f(worldVecR[0], -worldVecR[1], worldVecR[2]);
//	}
//	
//	public void test(MatrixTrackingGL gl) {
//		for (int z = -1; z >= -4; z--) {
//    		test(gl, new Vector3f(0, 0, z));
//    		test(gl, new Vector3f(mRight, mTop, z));
//    		test(gl, new Vector3f(mLeft, mTop, z));
//    		test(gl, new Vector3f(mRight, mBottom, z));
//    		test(gl, new Vector3f(mLeft, mBottom, z));
//		}
//	}
//	
//	public void test(MatrixTrackingGL gl, Vector3f vec) {
//		Vector3f vecWin;
//		// Forward
//		
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		float [] modelview = new float[16];
//		gl.getMatrix(modelview, 0);
//		
//		gl.glMatrixMode(GL10.GL_PROJECTION);
//		float [] projection = new float[16];
//		gl.getMatrix(projection, 0);
//		
//		float clipW;
//		
//		float [] worldVec = new float[4];
//		worldVec[0] = vec.getX();
//		worldVec[1] = vec.getY();
//		worldVec[2] = vec.getZ();
//		worldVec[3] = 1;
//
//		float [] eyeVec = new float[4];
//		Matrix.multiplyMV(eyeVec, 0, modelview, 0, worldVec, 0);
//
//		float [] clipVec = new float[4];
//		Matrix.multiplyMV(clipVec, 0, projection, 0, eyeVec, 0);
//		
//		float [] normDevCoords = new float[4];
//		float div = 1f/clipVec[3];
//		normDevCoords[0] = clipVec[0] * div;
//		normDevCoords[1] = clipVec[1] * div;
//		normDevCoords[2] = clipVec[2] * div;
//		normDevCoords[3] = 1f;
//		
//		float [] winCoords = new float[3];
//		winCoords[0] = (normDevCoords[0] *.5f + .5f) * mWidth;
//		winCoords[1] = (normDevCoords[1] *.5f + .5f) * mHeight;
//		winCoords[2] = (1 + normDevCoords[2]) * .5f;
//
//		vecWin = new Vector3f(winCoords[0], winCoords[1], winCoords[2]);
//		clipW = clipVec[3];
//		
//		Log.d("DEBUG", "FORWARD " + vec.toString());
//		Log.d("DEBUG", " ->winCoords=" + vecWin.toString() + ", clipW=" + clipW);
//		
//		// Reverse
//		Vector3f vecWorld;
//		float vecW;
//		float [] winCoordsR = new float[3];
//		float z = -worldVec[2];
//		winCoordsR[0] = vecWin.getX();
//		winCoordsR[1] = vecWin.getY();
//		winCoordsR[2] = 1f - 1f/z + 1f/(z*1000) + 1f/(z*1000000);
//
//		float [] normDevCoordsR = new float[4];
//		normDevCoordsR[0] = ((winCoordsR[0] / mWidth) - .5f)/.5f;
//		normDevCoordsR[1] = ((winCoordsR[1] / mHeight) - .5f)/.5f;
//		normDevCoordsR[2] = ((winCoordsR[2] / .5f - 1));
//		normDevCoordsR[3] = 1f;
//
//		float [] clipVecR = new float[4];
//		clipVecR[0] = normDevCoordsR[0] * clipW;
//		clipVecR[1] = normDevCoordsR[1] * clipW;
//		clipVecR[2] = normDevCoordsR[2] * clipW;
//		clipVecR[3] = z;
//		
//		float [] scratch = new float[16];
//		Matrix.multiplyMM(scratch, 0, projection, 0, modelview, 0);
//		Matrix.invertM(scratch, 0, scratch, 0);
//
//		float [] worldVecR = new float[4];
//		Matrix.multiplyMV(worldVecR, 0, scratch, 0, clipVecR, 0);
//
//		vecWorld = new Vector3f(worldVecR[0], worldVecR[1], worldVecR[2]);
//		vecW = worldVecR[3];
//		Log.d("DEBUG", "REVERSE " + vecWorld.toString() + ", W=" + vecW);
//	}
	
//	public Vector3f getWorldPosition(int px, int py) {
//		Vector3f vec = new Vector3f();
//		float wWidth = mRight - mLeft;
//		float wHeight = mTop - mBottom;
//		vec.setX((wWidth * (float)px/(float)mWidth) + mLeft);
//		vec.setY((wHeight * (float)(mHeight-py)/(float)mHeight) + mBottom);
//		return vec;
//	}
	
//	public Vector3f getUnproject(MatrixTrackingGL gl, float winX, float winY) {
//		float [] modelview = new float[16];
//		float [] projection = new float[16];
//		int [] view = new int[4];
//		float [] obj = new float[4];
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		gl.getMatrix(modelview, 0);
//		gl.glMatrixMode(GL10.GL_PROJECTION);
//		gl.getMatrix(projection, 0);
//		view[0] = 0;
//		view[1] = 0;
//		view[2] = mWidth;
//		view[3] = mHeight;
//		GLU.gluUnProject(
//				winX, (mHeight-winY), 1, 
//				modelview, 0, 
//				projection, 0, view, 0, obj, 0);
//		return new Vector3f(obj[0], obj[1], obj[2]);
//	}
	
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
	
	public void setOrtho() {
		mDoOrtho = true;
	}
	
	public void setPerspective() {
		mDoOrtho = false;
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
	
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[Lf,Rt,Bt,Tp]=");
		sbuf.append(mLeft);
		sbuf.append(",");
		sbuf.append(mRight);
		sbuf.append(",");
		sbuf.append(mBottom);
		sbuf.append(",");
		sbuf.append(mTop);
		sbuf.append(", Up=");
		sbuf.append(mUp.toString());
		return sbuf.toString();
	}
}