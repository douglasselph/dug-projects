package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderMap extends ControlRenderer
{
	Map	mMap;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		mMap.onDraw(gl);
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
