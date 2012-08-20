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
	Vector3f		mRotate;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
		mRotate = new Vector3f();
	}

	@Override
	public void scale(float delta)
	{
		mCamera.scale(delta, mMaxZ);
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		// gl.glRotatef(angle, x, y, z);
		mCamera.applyViewBounds(gl);
		mMap.onDraw(gl);
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
		mCamera.pan(xDelta, yDelta, mMaxBounds);
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
