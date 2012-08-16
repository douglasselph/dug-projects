package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class Camera
{

	static final Boolean	LOG			= false;
	static final String		TAG			= "Camera";

	protected float			mNearPlane	= 1;
	protected float			mFarPlane	= 1000;
	protected float			mAngle		= 65.0f;
	protected int			mHeight		= 100;
	protected int			mWidth		= 100;
	/** left clipping plane of viewport in model space coordinates */
	protected float			mLeft;
	/** right clipping plane */
	protected float			mRight;
	/** top clipping plane */
	protected float			mTop;
	/** bottom clipped plane */
	protected float			mBottom;
	protected boolean		mDoOrtho;

	public Camera()
	{
	}

	/**
	 * Called to defined the frustrum of the camera. Called rarely.
	 * 
	 * @param gl
	 */
	public void setPerspective(GL10 gl)
	{
		gl.glViewport(0, 0, mWidth, mHeight);
		gluPerspective(gl, mAngle, (float) mWidth / mHeight, mNearPlane, mFarPlane);
	}

	public float getHeight()
	{
		return mHeight;
	}

	public float getWidth()
	{
		return mWidth;
	}

	/**
	 * Doing it myself, so I can record the computed clipping planes.
	 * 
	 * @param gl
	 * @param fovy
	 * @param aspect
	 * @param zNear
	 * @param zFar
	 */
	void gluPerspective(GL10 gl, float fovy, float aspect, float zNear, float zFar)
	{
		mTop = zNear * (float) Math.tan(fovy * (Math.PI / 360.0));
		mBottom = -mTop;
		mLeft = mBottom * aspect;
		mRight = mTop * aspect;

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		if (mDoOrtho)
		{
			gl.glOrthof(mLeft, mRight, mBottom, mTop, zNear, zFar);
			if (LOG)
			{
				Log.i(TAG, "glOrtho(" + mLeft + ", " + mRight + ", " + mBottom + ", " + mTop + ", " + zNear + ", "
						+ zFar + ")");
			}
		}
		else
		{
			gl.glFrustumf(mLeft, mRight, mBottom, mTop, zNear, zFar);
			if (LOG)
			{
				Log.i(TAG, "glFrustum(" + mLeft + ", " + mRight + ", " + mBottom + ", " + mTop + ", " + zNear + ", "
						+ zFar + ")");
			}
		}
	}

	public Camera setOrtho()
	{
		mDoOrtho = true;
		return this;
	}

	public Camera setPerspective()
	{
		mDoOrtho = false;
		return this;
	}

	public Camera setNearFar(float near, float far)
	{
		mNearPlane = near;
		mFarPlane = far;
		return this;
	}

	public Camera setScreenDimension(int width, int height)
	{
		mWidth = width;
		mHeight = height;
		return this;
	}

	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[Lf,Rt,Bt,Tp]=");
		sbuf.append(mLeft);
		sbuf.append(",");
		sbuf.append(mRight);
		sbuf.append(",");
		sbuf.append(mBottom);
		sbuf.append(",");
		sbuf.append(mTop);
		return sbuf.toString();
	}
}