package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.tipsolutions.jacket.math.Bounds2D;

public class Camera
{

	static final Boolean	LOG	= true;
	static final String		TAG	= "Camera";

	static float LARGER(float v1, float v2)
	{
		return v1 >= v2 ? v1 : v2;
	}

	protected float		mNearPlane	= 1;
	protected float		mFarPlane	= 1000;
	protected float		mAngle		= 65.0f;
	protected float		mAspect		= 0.5f;
	protected int		mHeight		= 100;
	protected int		mWidth		= 100;
	protected Bounds2D	mClippingPlane;

	protected boolean	mDoOrtho;

	public Camera()
	{
	}

	/**
	 * Return the boundaries for an object at the given distance.
	 * 
	 * @param dist
	 * @return
	 */
	public Bounds2D getBounds(float dist)
	{
		Bounds2D bounds = new Bounds2D();
		bounds.setMaxY(dist * (float) Math.tan(mAngle * (Math.PI / 360.0)));
		bounds.setMinY(-bounds.getMaxY());
		bounds.setMinX(bounds.getMinY() * mAspect);
		bounds.setMaxX(bounds.getMaxY() * mAspect);
		return bounds;
	}

	/**
	 * Return the distance we have to be from the camera in order to just see the object with the given bounds
	 * completely.
	 * 
	 * @param bounds
	 * @return
	 */
	public float getDist(Bounds2D bounds)
	{
		// Find most extreme edge
		float maxY = Math.abs(bounds.getMaxY());
		float minY = Math.abs(bounds.getMinY());
		float yValue = LARGER(minY, maxY);
		float maxX = Math.abs(bounds.getMaxX());
		float minX = Math.abs(bounds.getMinX());
		float xValue = LARGER(minX, maxX);

		float factor = (float) Math.tan(mAngle * (Math.PI / 360.0));

		if (yValue >= xValue)
		{
			return yValue / factor;
		}
		return xValue / factor / mAspect;
	}

	public float getHeight()
	{
		return mHeight;
	}

	public float getWidth()
	{
		return mWidth;
	}

	public Camera setNearFar(float near, float far)
	{
		mNearPlane = near;
		mFarPlane = far;
		return this;
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

	/**
	 * Called to defined the frustrum of the camera. Called rarely.
	 * 
	 * @param gl
	 */
	public void setPerspective(GL10 gl)
	{
		gl.glViewport(0, 0, mWidth, mHeight);

		mAspect = (float) mWidth / mHeight;
		mClippingPlane = getBounds(mNearPlane);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		if (mDoOrtho)
		{
			gl.glOrthof(mClippingPlane.getMinX(), mClippingPlane.getMaxX(), mClippingPlane.getMinY(),
					mClippingPlane.getMaxY(), mNearPlane, mFarPlane);
			if (LOG)
			{
				Log.i(TAG, "glOrtho(" + mNearPlane + ", " + mFarPlane + ", " + mClippingPlane.toString() + ")");
			}
		}
		else
		{
			gl.glFrustumf(mClippingPlane.getMinX(), mClippingPlane.getMaxX(), mClippingPlane.getMinY(),
					mClippingPlane.getMaxY(), mNearPlane, mFarPlane);
			if (LOG)
			{
				Log.i(TAG, "glFrustum(" + mNearPlane + ", " + mFarPlane + ", " + mClippingPlane.toString() + ")");
			}
		}
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
		sbuf.append(mClippingPlane.getMinX());
		sbuf.append(",");
		sbuf.append(mClippingPlane.getMaxX());
		sbuf.append(",");
		sbuf.append(mClippingPlane.getMinY());
		sbuf.append(",");
		sbuf.append(mClippingPlane.getMaxY());
		return sbuf.toString();
	}
}