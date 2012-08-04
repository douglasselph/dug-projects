package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class TestSquareRenderer extends ControlRenderer
{
	Context		mCtx;
	TestSquare	mTestSquare;
	float		mAngle;

	public TestSquareRenderer(Context context, ControlSurfaceView view)
	{
		super(view);
		mCtx = context;
		mTestSquare = new TestSquare();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		mTestSquare.loadTexture(gl, mCtx, R.drawable.sample);
	}

	@Override
	public void onDrawFrame(MatrixTrackingGL gl)
	{
		super.onDrawFrame(gl);

		gl.glTranslatef(0, 0, -3.0f);
		gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
		mTestSquare.draw(gl);
		mAngle += 1.2f;
	}
}
