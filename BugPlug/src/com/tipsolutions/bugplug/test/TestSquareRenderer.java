package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;

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
		gl.glTranslatef(0, 0, -10.4f);
		gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
		mTestSquare.draw(gl);
		mAngle += 1.2f;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		if (height == 0)
		{ // Prevent A Divide By Zero By
			height = 1; // Making Height Equal One
		}
		gl.glViewport(0, 0, width, height); // Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); // Select The Projection Matrix
		gl.glLoadIdentity(); // Reset The Projection Matrix

		// Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f, 100.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); // Select The Modelview Matrix
		gl.glLoadIdentity(); // Reset The Modelview Matrix
	}

}
