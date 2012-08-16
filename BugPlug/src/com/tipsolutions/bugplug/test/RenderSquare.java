package com.tipsolutions.bugplug.test;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderSquare extends ControlRenderer
{
	Square2	mSquare;
	float	mAngle;

	public RenderSquare(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);
		mSquare = new Square2(1);

		if (tm == null)
		{
			mSquare.setColor(Color4f.BLUE);
		}
		else
		{
			mSquare.setTexture(tm.getTexture(R.drawable.sample));
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

	@Override
	public String toString()
	{
		return mSquare.toString();
	}
}
