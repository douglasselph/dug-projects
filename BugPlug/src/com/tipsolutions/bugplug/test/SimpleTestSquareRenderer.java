package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import com.tipsolutions.bugplug.MyApplication;
import com.tipsolutions.bugplug.R;

public class SimpleTestSquareRenderer implements GLSurfaceView.Renderer
{
	Context		mCtx;
	TestSquare	mSquare;
	float		mAngle;

	public SimpleTestSquareRenderer(Context context)
	{
		mCtx = context;
		mSquare = new TestSquare();
	}

	public void onDrawFrame(GL10 gl)
	{
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		gl.glTranslatef(0, 0, -11.0f);
		gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
		mSquare.draw(gl);
		mAngle += 1.2f;
	}

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

	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); // Black Background
		gl.glClearDepthf(1.0f); // Depth Buffer Setup

		mSquare.loadTexture(gl, MyApplication.getTM(mCtx), R.drawable.sample);
	}
}
