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

import com.tipsolutions.jacket.image.ImageUtils;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;

public class ControlRenderer implements GLSurfaceView.Renderer
{

	public interface OnAfterNextRender
	{
		void run(ControlRenderer renderer, MatrixTrackingGL gl);
	};

	protected final ControlSurfaceView	mView;
	protected Color4f					mBackground;
	protected int						mWidth;
	protected int						mHeight;
	protected final Camera				mCamera;
	protected MatrixTrackingGL			mLastGL;
	protected OnAfterNextRender			mOnAfterNextRender;
	protected boolean					mRenderWhenDirty;

	public ControlRenderer(ControlSurfaceView view)
	{
		mView = view;
		mCamera = new Camera();
	}

	protected void clearScene(MatrixTrackingGL gl)
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

	public MatrixTrackingGL getLastGL()
	{
		return mLastGL;
	}

	public float getWidth()
	{
		return mWidth;
	}

	protected void onCreatedInitDepth(GL10 gl)
	{
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
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
		gl.glEnable(GL10.GL_TEXTURE_2D);
	}

	public void onDrawFrame(GL10 gl)
	{
		MatrixTrackingGL mgl;
		if (gl instanceof MatrixTrackingGL)
		{
			mgl = (MatrixTrackingGL) gl;
		}
		else
		{
			mgl = new MatrixTrackingGL(gl);
		}
		mLastGL = mgl;
		clearScene(mgl);

		mgl.glMatrixMode(GL10.GL_MODELVIEW);
		mgl.glLoadIdentity();

		onDrawFrame(mgl);
		onDrawFrameDone(mgl);
	}

	protected void onDrawFrame(MatrixTrackingGL gl)
	{
	}

	protected void onDrawFrameDone(MatrixTrackingGL gl)
	{
		if (mOnAfterNextRender != null)
		{
			mOnAfterNextRender.run(this, gl);
			mOnAfterNextRender = null;
		}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		mWidth = width;
		mHeight = height;

		mCamera.setScreenDimension(mWidth, mHeight);
		mCamera.setNearFar(1, 10);
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
			gl.glClearColor(mBackground.getRed(), mBackground.getGreen(),
					mBackground.getBlue(), mBackground.getAlpha());
		}
		else
		{
			gl.glClearColor(1f, 1f, 1f, 1f);
		}
		// gl.glDisable(GL10.GL_DITHER);

		onCreatedInitDepth(gl);
		onCreatedInitShading(gl);
		onCreatedInitTexture(gl);
		onCreatedInitHint(gl);
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

	public void setOnAfterNextRender(OnAfterNextRender run)
	{
		mOnAfterNextRender = run;
	}

	public void setRenderOnDirty()
	{
		mRenderWhenDirty = true;
		mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	public Bitmap snapshot(MatrixTrackingGL gl)
	{
		int width = mWidth;
		int height = mHeight;
		int size = width * height;
		ByteBuffer buf = ByteBuffer.allocateDirect(size * 4);
		buf.order(ByteOrder.nativeOrder());
		gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA,
				GL10.GL_UNSIGNED_BYTE, buf);
		int data[] = new int[size];
		buf.asIntBuffer().get(data);
		buf = null;
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.RGB_565);
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

	public void snapshot(MatrixTrackingGL gl, File file) throws IOException
	{
		Bitmap bitmap = snapshot(gl);
		ImageUtils.SaveBitmap(bitmap, file);
	}

}
