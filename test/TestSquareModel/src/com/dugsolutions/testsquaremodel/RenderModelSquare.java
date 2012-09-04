package com.dugsolutions.testsquaremodel;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.view.ControlRenderer;
import com.dugsolutions.jacket.view.ControlSurfaceView;

public class RenderModelSquare extends ControlRenderer
{
	ModelSquare	mSquare;
	float		mAngle;

	public RenderModelSquare(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);
		mSquare = new ModelSquare(1);

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
	protected void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

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
