package com.tipsolutions.testcube;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class CubeRenderer extends ControlRenderer
{
	Cube	mCube;
	float	mAngle;

	public CubeRenderer(ControlSurfaceView view)
	{
		super(view, null);
		mCube = new Cube();
	}

	@Override
	protected void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		gl.glTranslatef(0, 0, -3.0f);

		gl.glRotatef(mAngle, 0, 1, 0);
		gl.glRotatef(mAngle * 0.25f, 1, 0, 0);

		mCube.draw(gl);

		gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
		gl.glTranslatef(0.5f, 0.5f, 0.5f);

		mCube.draw(gl);

		mAngle += 1.2f;
	}
}
