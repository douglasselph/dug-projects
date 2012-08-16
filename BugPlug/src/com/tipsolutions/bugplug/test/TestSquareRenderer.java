package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class TestSquareRenderer extends ControlRenderer
{
	TestSquare	mTestSquare;
	float		mAngle;

	public TestSquareRenderer(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);
		mTestSquare = new TestSquare();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		mTestSquare.loadTexture(gl, mTM, R.drawable.sample);
	}

	@Override
	public void onDrawFrame(GL10 gl)
	{
		clearScene(gl);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0, 0, -4f);
		gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
		mTestSquare.draw(gl);
		mAngle += 1.2f;
	}

}
