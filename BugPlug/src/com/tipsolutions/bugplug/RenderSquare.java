package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Square;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderSquare extends ControlRenderer
{
	Context	mCtx;
	Square	mSquare;
	float	mAngle;
	boolean	mWithTexture;

	public RenderSquare(Context context, ControlSurfaceView view,
			boolean withTex)
	{
		super(view);
		mCtx = context;
		mSquare = new Square(1);
		mWithTexture = withTex;

		if (!mWithTexture)
		{
			mSquare.setColor(Color4f.BLUE);
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		if (mWithTexture)
		{
			mSquare.setTexture(MyApplication.getTM(mCtx).getTexture(
					R.drawable.sample));
			mSquare.getTexture().load(gl);
		}
	}

	@Override
	public void onDrawFrame(MatrixTrackingGL gl)
	{
		super.onDrawFrame(gl);

		gl.glTranslatef(0, 0, -3.0f);
		gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
		mSquare.onDraw(gl);
		mAngle += 1.2f;
	}
}
