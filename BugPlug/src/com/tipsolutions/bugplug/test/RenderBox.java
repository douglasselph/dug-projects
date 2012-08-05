package com.tipsolutions.bugplug.test;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Box;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderBox extends ControlRenderer
{
	Box		mBox;
	float	mAngle;

	public RenderBox(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mBox = new Box(1f);

		if (tm == null)
		{
			mBox.setColor(new Color4f(1f, 0f, 0f, 0.5f));
		}
		else
		{
			mBox.setTexture(mTM.getTexture(R.drawable.sample));
		}
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

		mBox.onDraw(gl);

		gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
		gl.glTranslatef(0.5f, 0.5f, 0.5f);

		mBox.onDraw(gl);

		mAngle += 1.2f;
	}
}
