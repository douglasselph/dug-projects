package com.tipsolutions.testbox;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.model.Box;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class RenderBox extends ControlRenderer
{
	Box		mBox;
	float	mAngle;

	public RenderBox(ControlSurfaceView view, boolean useMaterials, TextureManager tm)
	{
		super(view, tm);

		mBox = new Box(1f);

		if (tm == null && !useMaterials)
		{
			mBox.setColor(new Color4f(1f, 0f, 0f, 0.5f));
		}
		else if (tm != null)
		{
			mBox.setTexture(mTM.getTexture(R.drawable.sample));
		}
		if (useMaterials)
		{
			// mBox.setColorAmbient(Color4f.RED);
			mBox.setColorDiffuse(Color4f.BLUE);
			// mBox.setColorSpecular(Color4f.GREEN);
			// mBox.setColorEmission(new Color4f(0.3f, 0f, 0f, 0.2f));
			// mBox.setColorShininess(shininess);
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		initDepth(gl);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		// gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, new Vector4f(0f, 0f, 1f, 0).toArray(), 0);
		// gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, Color4f.RED.toArray(), 0);
	}

	@Override
	protected void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		gl.glPushMatrix();
		gl.glTranslatef(0, 0, -3.0f);

		gl.glRotatef(mAngle, 0, 1, 0);
		gl.glRotatef(mAngle * 0.25f, 1, 0, 0);

		mBox.onDraw(gl);

		gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
		gl.glTranslatef(0.5f, 0.5f, 0.5f);

		mBox.onDraw(gl);

		gl.glPopMatrix();

		mAngle += 0.2f;
	}
}
