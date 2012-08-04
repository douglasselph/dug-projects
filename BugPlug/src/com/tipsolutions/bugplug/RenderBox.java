package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Box;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderBox extends ControlRenderer
{
	Context	mCtx;
	Box		mBox;
	float	mAngle;
	boolean	mWithTex;

	public RenderBox(Context context, ControlSurfaceView view, boolean withTex)
	{
		super(view);

		mCtx = context;
		mBox = new Box(1f);
		mWithTex = withTex;

		if (!mWithTex)
		{
			mBox.setColor(new Color4f(1f, 0f, 0f, 0.5f));
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		if (mWithTex)
		{
			mBox.setTexture(MyApplication.getTM(mCtx).getTexture(
					R.drawable.sample));
			mBox.getTexture().load(gl);
		}
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
