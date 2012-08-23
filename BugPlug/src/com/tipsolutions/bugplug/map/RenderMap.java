package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.view.MotionEvent;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.Vector4f;
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
	Vector4f		mSunPos;
	boolean			mIsPan	= true;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
		mRotateAngle = new Vector3f();
		mSunPos = new Vector4f(0, 0, -1, 1);
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

	public boolean isPan()
	{
		return mIsPan;
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		mCamera.applyViewBounds(gl);
		applyRotate(gl);
		mMap.onDraw(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		super.onSurfaceChanged(gl, width, height);

		mCamera.setViewBounds(mMap.getBounds());
		mMaxZ = mCamera.getViewingLoc().getZ();
		mMaxBounds = new Bounds2D(mCamera.getViewBounds());
		mMaxBounds.setMinX(mMaxBounds.getMinX() * 1.4f);
		mMaxBounds.setMaxX(mMaxBounds.getMaxX() * 1.4f);
		mSunPos.set(mMaxBounds.getSizeX() / 2, 0, -1f, 1);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		initDepth(gl);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnable(GL10.GL_LIGHTING);
		/* AMBIENT LIGHT */
		gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, new Color4f(0.25f, 0.25f, 0.25f, 1).toArray(), 0);
		/* GENERAL LIGHT */
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, new Vector4f(0f, 0f, 1f, 0).toArray(), 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, new Vector4f(0.5f, 0.5f, 0.5f, 1f).toArray(), 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, Color4f.WHITE.toArray(), 0);
		/* SPECULAR HIGHLIGHT */
		gl.glEnable(GL10.GL_LIGHT1);
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, mSunPos.toArray(), 0);
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPECULAR, Color4f.WHITE.toArray(), 0);
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

	public void resetView()
	{
		mRotateAngle = new Vector3f();
		mIsPan = true;
		mCamera.setViewBounds(mMap.getBounds());
		mView.requestRender();
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
