package com.tipsolutions.jacket.view;

import javax.microedition.khronos.opengles.GL;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.tipsolutions.jacket.math.MatrixTrackingGL;

/**
 * Like a regular surface view but provides for touch event support.
 * 
 * @author dug
 */
public class ControlSurfaceView extends GLSurfaceView implements IView
{

	// final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	// final float TRACKBALL_SCALE_FACTOR = 36.0f;
	IEventTap		mEventTap	= null;
	// final Controller mController;
	ControlRenderer	mRenderer;

	class MyGLWrapper implements GLWrapper
	{
		public GL wrap(GL gl)
		{
			return new MatrixTrackingGL(gl);
		}
	};

	public ControlSurfaceView(Context context)
	{
		super(context);
		setGLWrapper(new MyGLWrapper());
		// mController = new Controller(control, this);
	}

	public void setEventTap(IEventTap eventTap)
	{
		mEventTap = eventTap;
	}

	public Renderer getRenderer()
	{
		return mRenderer;
	}

	public void setRenderer(ControlRenderer renderer)
	{
		mRenderer = renderer;
		super.setRenderer(renderer);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		requestRender();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent e)
	{
		boolean changed = false;
		if (mEventTap != null)
		{
			float x = e.getX();
			float y = e.getY();
			switch (e.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					changed = mEventTap.pressDown(x, y);
					break;
				case MotionEvent.ACTION_MOVE:
					changed = mEventTap.pressMove(x, y);
					break;
				case MotionEvent.ACTION_UP:
					changed = mEventTap.pressUp(x, y);
					break;
			}
			if (changed)
			{
				requestRender();
			}
		}
		return changed;
	}

	// public Bitmap snapshot() {
	// Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
	// Bitmap.Config.ARGB_8888);
	// Canvas canvas = new Canvas(bitmap);
	// draw(canvas);
	// return bitmap;
	// }
	//
	// public void snapshot(File file) throws IOException {
	// Bitmap bitmap = snapshot();
	// FileOutputStream fos = new FileOutputStream(file);
	// bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
	// fos.flush();
	// fos.close();
	// }

	// public void setOnAfterNextRender(OnAfterNextRender run)
	// {
	// mRenderer.setOnAfterNextRender(run);
	// requestRender();
	// }

}
