package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Vector3f;

public class Camera
{

	static final Boolean	LOG	= true;
	static final String		TAG	= "Camera";

	static float LARGER(float v1, float v2)
	{
		return v1 >= v2 ? v1 : v2;
	}

	static float LESSER(float v1, float v2)
	{
		return v1 <= v2 ? v1 : v2;
	}

	protected float		mNearPlane	= 1;
	protected float		mFarPlane	= 1000;
	protected float		mAngle		= 65.0f;
	protected float		mAspect		= 0.5f;
	protected int		mHeight		= 100;
	protected int		mWidth		= 100;
	protected Bounds2D	mClippingPlane;
	protected Vector3f	mViewingLoc	= new Vector3f();

	protected boolean	mDoOrtho;

	public Camera()
	{
	}

	/**
	 * Apply a transformation so the view bounds specified by setViewBounds() is visible.
	 * 
	 * @param gl
	 */
	public void applyViewBounds(GL10 gl)
	{
		gl.glTranslatef(mViewingLoc.getX(), mViewingLoc.getY(), mViewingLoc.getZ());
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

	public Bounds2D getClippingPlane()
	{
		return mClippingPlane;
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

		if (yValue < xValue)
		{
			return yValue / factor;
		}
		return xValue / factor / mAspect;
	}

	public int getHeight()
	{
		return mHeight;
	}

	public Vector3f getViewingLoc()
	{
		return mViewingLoc;
	}

	public Bounds2D getViewBounds()
	{
		return getBounds(-mViewingLoc.getZ());
	}

	public int getWidth()
	{
		return mWidth;
	}

	/**
	 * 
	 * @param xDelta
	 *        : percentage to adjust in X
	 * @param yDelta
	 *        : percentage to adjust in Y
	 * @param worldLimit
	 *        : The limit
	 */
	public void pan(float xDelta, float yDelta, Bounds2D worldLimit)
	{
		Bounds2D curBounds = getBounds(-mViewingLoc.getZ());

		float sizeX = curBounds.getSizeX();
		float sizeY = curBounds.getSizeY();
		float adjustX = sizeX * xDelta;
		float adjustY = sizeY * yDelta;

		mViewingLoc.addX(adjustX);
		mViewingLoc.addY(-adjustY);

		curBounds.add(-mViewingLoc.getX(), -mViewingLoc.getY());

		float diff;

		if (curBounds.getMinX() < worldLimit.getMinX())
		{
			diff = curBounds.getMinX() - worldLimit.getMinX();
			mViewingLoc.addX(diff);
		}
		else if (curBounds.getMaxX() > worldLimit.getMaxX())
		{
			diff = curBounds.getMaxX() - worldLimit.getMaxX();
			mViewingLoc.addX(diff);
		}
		if (curBounds.getMinY() < worldLimit.getMinY())
		{
			diff = curBounds.getMinY() - worldLimit.getMinY();
			mViewingLoc.addY(diff);
		}
		else if (curBounds.getMaxY() > worldLimit.getMaxY())
		{
			diff = curBounds.getMaxY() - worldLimit.getMaxY();
			mViewingLoc.addY(diff);
		}
	}

	/**
	 * Ensure the viewing location is such that if the range sees more than the limit in one of the directions,
	 * it will clamp down so that it is upper-left justified.
	 */
	public void floor(Bounds2D limit)
	{

	}

	public void scale(float scale, float maxZ)
	{
		float newZ = mViewingLoc.getZ() * scale;

		if (newZ > -mNearPlane)
		{
			newZ = -mNearPlane;
		}
		else if (newZ < maxZ)
		{
			newZ = maxZ;
			Log.d("DEBUG", "maxZ=" + maxZ);

		}
		mViewingLoc.setZ(newZ);
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

	/**
	 * Set the viewing location such that the passed in bounds is just seen.
	 * Used in conjuction with applyViewBounds().
	 * 
	 * @param bounds
	 *        : area staring at.
	 */
	public void setViewBounds(Bounds2D bounds)
	{
		float centerX = (bounds.getMaxX() + bounds.getMinX()) / 2;
		float centerY = (bounds.getMaxY() + bounds.getMinY()) / 2;
		float halfSizeX = bounds.getSizeX() / 2;
		float halfSizeY = bounds.getSizeY() / 2;
		Bounds2D centerBounds = new Bounds2D(-halfSizeX, -halfSizeY, halfSizeX, halfSizeY);
		mViewingLoc.setX(-centerX);
		mViewingLoc.setY(-centerY);
		mViewingLoc.setZ(-getDist(centerBounds));
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