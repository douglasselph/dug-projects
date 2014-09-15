package com.dugsolutions.jacket.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * Like a regular surface view but provides for touch event support.
 * 
 * @author dug
 */
public class ControlSurfaceView extends GLSurfaceView implements IView
{
	ControlRenderer	mRenderer;

	public ControlSurfaceView(Context context)
	{
		super(context);
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
		mRenderer.setRebuildTextures();
		requestRender();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev)
	{
		boolean changed = false;
		if (mRenderer.onTouchEvent(ev))
		{
			changed = true;
			requestRender();
		}
		return changed;
	}

	public Bitmap snapshot()
	{
		Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		draw(canvas);
		return bitmap;
	}

	public void snapshot(File file) throws IOException
	{
		Bitmap bitmap = snapshot();
		FileOutputStream fos = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		fos.flush();
		fos.close();
	}

}
