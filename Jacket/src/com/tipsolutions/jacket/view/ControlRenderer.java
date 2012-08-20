package com.tipsolutions.jacket.view;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.tipsolutions.jacket.image.ImageUtils;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;

public class ControlRenderer implements GLSurfaceView.Renderer, IEventTap
{
	static final String					TAG	= "ControlRenderer";
	static final Boolean				ERR	= true;				// If set to true show errors from opengl.

	// public interface OnAfterNextRender
	// {
	// void run(ControlRenderer renderer, MatrixTrackingGL gl);
	// };

	protected final TextureManager		mTM;
	protected final ControlSurfaceView	mView;
	protected Color4f					mBackground;
	protected int						mWidth;
	protected int						mHeight;
	protected final Camera				mCamera;
	// protected OnAfterNextRender mOnAfterNextRender;
	protected boolean					mRenderWhenDirty;

	public ControlRenderer(ControlSurfaceView view, TextureManager tm)
	{
		mTM = tm;
		mView = view;
		mCamera = new Camera();
	}

	protected void clearScene(GL10 gl)
	{
		// clear the color buffer to show the ClearColor we called above...
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	public Color4f getBackground()
	{
		return mBackground;
	}

	public float getHeight()
	{
		return mHeight;
	}

	public float getWidth()
	{
		return mWidth;
	}

	protected void onCreatedInitDepth(GL10 gl)
	{
		// gl.glClearDepthf(1.0f);
		// gl.glEnable(GL10.GL_DEPTH_TEST);
		// gl.glDepthFunc(GL10.GL_LEQUAL);
	}

	protected void onCreatedInitHint(GL10 gl)
	{
		/*
		 * By default, OpenGL enables features that improve quality but reduce
		 * performance. One might want to tweak that especially on software
		 * renderer.
		 */
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
	}

	protected void onCreatedInitShading(GL10 gl)
	{
		gl.glShadeModel(GL10.GL_SMOOTH);
	}

	protected void onCreatedInitTexture(GL10 gl)
	{
		// gl.glEnable(GL10.GL_TEXTURE_2D);
	}

	public void onDrawFrame(GL10 gl)
	{
		onDrawFrameStart(gl);
		onDrawFrameContents(gl);
		onDrawFrameDone(gl);
	}

	protected void onDrawFrameStart(GL10 gl)
	{
		clearScene(gl);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	protected void onDrawFrameContents(GL10 gl)
	{

	}

	protected void onDrawFrameDone(GL10 gl)
	{
		if (ERR)
		{
			printErrors(gl);
		}
		// if (mOnAfterNextRender != null)
		// {
		// mOnAfterNextRender.run(this, gl);
		// mOnAfterNextRender = null;
		// }
	}

	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		mWidth = width;
		mHeight = height;

		mCamera.setScreenDimension(mWidth, mHeight);
		mCamera.setPerspective(gl);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		if (mRenderWhenDirty)
		{
			mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}
		if (mBackground != null)
		{
			// define the color we want to be displayed as the "clipping wall"
			gl.glClearColor(mBackground.getRed(), mBackground.getGreen(), mBackground.getBlue(), mBackground.getAlpha());
		}
		else
		{
			gl.glClearColor(1f, 1f, 1f, 1f);
		}
		if (mTM != null)
		{
			mTM.load(gl);
		}

		// gl.glDisable(GL10.GL_DITHER);

		onCreatedInitDepth(gl);
		onCreatedInitShading(gl);
		onCreatedInitTexture(gl);
		onCreatedInitHint(gl);
	}

	public boolean onTouchEvent(MotionEvent ev)
	{
		return false;
	}

	protected void printErrors(GL10 gl)
	{
		int err;
		while ((err = gl.glGetError()) != GL10.GL_NO_ERROR)
		{
			printError(getError(err));
		}
	}

	protected void printError(String msg)
	{
		Log.e(TAG, msg);
	}

	static protected String getError(int code)
	{
		switch (code)
		{
			case GL10.GL_INVALID_ENUM:
				return "Invalid enum";
			case GL10.GL_INVALID_OPERATION:
				return "Invalid operation";
			case GL10.GL_INVALID_VALUE:
				return "Invalid value";
			case GL10.GL_STACK_OVERFLOW:
				return "Stack overflow";
			case GL10.GL_STACK_UNDERFLOW:
				return "Stack underflow";
			case GL10.GL_OUT_OF_MEMORY:
				return "out of memory";
		}
		return null;
	}

	/**
	 * Needs to happen before surface created
	 * 
	 * @param color
	 */
	public void setBackground(Color4f color)
	{
		mBackground = color;
	}

	// public void setOnAfterNextRender(OnAfterNextRender run)
	// {
	// mOnAfterNextRender = run;
	// }

	public void setRenderOnDirty()
	{
		mRenderWhenDirty = true;
		mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	public Bitmap snapshot(GL10 gl)
	{
		int width = mWidth;
		int height = mHeight;
		int size = width * height;
		ByteBuffer buf = ByteBuffer.allocateDirect(size * 4);
		buf.order(ByteOrder.nativeOrder());
		gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buf);
		int data[] = new int[size];
		buf.asIntBuffer().get(data);
		buf = null;
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		bitmap.setPixels(data, size - width, -width, 0, 0, width, height);
		data = null;

		short sdata[] = new short[size];
		ShortBuffer sbuf = ShortBuffer.wrap(sdata);
		bitmap.copyPixelsToBuffer(sbuf);
		for (int i = 0; i < size; ++i)
		{
			// BGR-565 to RGB-565
			short v = sdata[i];
			sdata[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
		}
		sbuf.rewind();
		bitmap.copyPixelsFromBuffer(sbuf);
		return bitmap;
	}

	public void snapshot(GL10 gl, File file) throws IOException
	{
		Bitmap bitmap = snapshot(gl);
		ImageUtils.SaveBitmap(bitmap, file);
	}

	public String toString()
	{
		return "";
	}
}
