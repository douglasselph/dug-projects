package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class CubeRenderer extends ControlRenderer
{
	Cube	mCube;
	float	mAngle;

	public CubeRenderer(ControlSurfaceView view)
	{
		super(view);
		mCube = new Cube();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onDrawFrame(MatrixTrackingGL gl)
	{
		super.onDrawFrame(gl);

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
