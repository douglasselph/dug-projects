package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;
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

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		float dist = mCamera.getDist(mMap.getBounds());
		gl.glTranslatef(0, 0, -dist);

		mMap.onDraw(gl);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		return mEventTap.onTouchEvent(ev);
	}

	public void pan(int xDelta, int yDelta)
	{
		Log.d("DEBUG", "PAN " + xDelta + ", " + yDelta);
	}

	public void scale(int xDelta, int yDelta)
	{
		Log.d("DEBUG", "SCALE " + xDelta + ", " + yDelta);
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
