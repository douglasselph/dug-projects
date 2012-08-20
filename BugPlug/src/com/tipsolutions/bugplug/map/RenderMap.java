package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import android.view.MotionEvent;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;
import com.tipsolutions.jacket.view.EventTapAdjust.Adjust;

public class RenderMap extends ControlRenderer implements Adjust
{
	Map				mMap;
	EventTapAdjust	mEventTap;
	float			mMaxZ;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
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

		mCamera.applyViewBounds(gl);
		mMap.onDraw(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		super.onSurfaceChanged(gl, width, height);

		mCamera.setViewBounds(mMap.getBounds());
		mMaxZ = mCamera.getViewingLoc().getZ();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		return mEventTap.onTouchEvent(ev);
	}

	public void pan(float xDelta, float yDelta)
	{
		mCamera.pan(xDelta, yDelta, mMap.getBounds());
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
