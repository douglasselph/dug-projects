package com.tipsolutions.bugplug.map;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
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
	protected void onDrawFrame(MatrixTrackingGL gl)
	{
		super.onDrawFrame(gl);

		gl.glTranslatef(0, 0, -3.0f);
		mMap.onDraw(gl);
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

}
