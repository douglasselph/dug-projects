package com.tipsolutions.bugplug;

import android.content.Context;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderMap extends ControlRenderer
{
	Map	mMap;

	public RenderMap(Context context, ControlSurfaceView view)
	{
		super(view);

		mMap = new Map(MyApplication.getTM(context));
	}

	@Override
	protected void onDrawFrame(MatrixTrackingGL gl)
	{
		super.onDrawFrame(gl);

		gl.glTranslatef(0, 0, -10.0f);
		mMap.onDraw(gl);
	}

}
