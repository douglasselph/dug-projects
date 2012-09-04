package com.dugsolutions.testcube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.view.ControlRenderer;
import com.dugsolutions.jacket.view.ControlSurfaceView;

public class CubeWRenderer extends ControlRenderer
{
	CubeW	mCube;
	float	mAngle;

	public CubeWRenderer(ControlSurfaceView view)
	{
		super(view, null);
		mCube = new CubeW();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		gl.glFrontFace(GL10.GL_CW);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_BACK);

		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
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
