package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import android.view.MotionEvent;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;
import com.tipsolutions.jacket.view.EventTapAdjust.Adjust;

public class RenderMap extends ControlRenderer implements Adjust
{
	Map				mMap;
	EventTapAdjust	mEventTap;
	float			mMaxZ;
	Bounds2D		mMaxBounds;
	Vector3f		mRotateAngle;
	boolean			mIsPan	= true;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
		mRotateAngle = new Vector3f();
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		mCamera.applyViewBounds(gl);
		applyRotate(gl);
		mMap.onDraw(gl);
	}

	void applyRotate(GL10 gl)
	{
		if (mRotateAngle.getX() != 0)
		{
			gl.glRotatef(mRotateAngle.getX(), 1, 0, 0);
		}
		if (mRotateAngle.getY() != 0)
		{
			gl.glRotatef(mRotateAngle.getY(), 0, 1, 0);
		}
		if (mRotateAngle.getZ() != 0)
		{
			gl.glRotatef(mRotateAngle.getZ(), 0, 0, 1);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		super.onSurfaceChanged(gl, width, height);

		mCamera.setViewBounds(mMap.getBounds());
		mMaxZ = mCamera.getViewingLoc().getZ();
		mMaxBounds = new Bounds2D(mCamera.getViewBounds());
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		return mEventTap.onTouchEvent(ev);
	}

	public void pan(float xDelta, float yDelta)
	{
		if (mIsPan)
		{
			mCamera.pan(xDelta, yDelta, mMaxBounds);
		}
		else
		{
			float delta;
			if (Math.abs(xDelta) > Math.abs(yDelta))
			{
				delta = xDelta;
			}
			else
			{
				delta = yDelta;
			}
			float degrees = (float) (180 * -delta);
			mRotateAngle.setZ(mRotateAngle.getZ() + degrees);
		}
	}

	@Override
	public void scale(float delta)
	{
		mCamera.scale(delta, mMaxZ);
	}

	public void setIsPan(boolean flag)
	{
		mIsPan = flag;
	}

	/**
	 * Indicate the tilt.
	 * 
	 * @param unit
	 *        : translates to 15 degrees per value.
	 */
	public void setTilt(float unit)
	{
		float degrees = unit * -15;
		mRotateAngle.setX(degrees);

		if (unit == 0)
		{
			mRotateAngle.setZ(0);
		}
		mView.requestRender();
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
