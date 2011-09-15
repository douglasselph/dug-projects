package com.tipsolutions.jacket.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;

public class ControlRenderer implements GLSurfaceView.Renderer {
	
	public interface OnAfterNextRender {
		void run(ControlRenderer renderer, MatrixTrackingGL gl);
	};
	
	protected final ControlSurfaceView mView;
	protected Color4f mClippingPlaneColor = null;
	protected int mWidth;
	protected int mHeight;
	protected final ControlCamera mCamera;
	protected MatrixTrackingGL mLastGL = null;
	protected OnAfterNextRender mOnAfterNextRender = null;
	
	public ControlRenderer(ControlSurfaceView view, ControlCamera camera) {
		mView = view;
		mCamera = camera;
	}
	
	public MatrixTrackingGL getLastGL() {
		return mLastGL;
	}
	
	public float getWidth() { return mWidth; }
	public float getHeight() { return mHeight; }

	@Override
	public void onDrawFrame(GL10 gl) {
		MatrixTrackingGL mgl = new MatrixTrackingGL(gl);
		mLastGL = mgl;
		
		clearScene(mgl);
		
		mgl.glMatrixMode(GL10.GL_PROJECTION);  // Modify the projection matrix 
		mgl.glLoadIdentity();
    	mCamera.applyFrustrum(mLastGL);
    	
		mgl.glMatrixMode(GL10.GL_MODELVIEW);  // Modify the modelview matrix in the following commands:
		mgl.glLoadIdentity();
		
		mgl.glFrontFace(GL10.GL_CCW); // Defines front face
		mgl.glEnable(GL10.GL_CULL_FACE);
		mgl.glCullFace(GL10.GL_BACK); // Do not draw this face
		mgl.glEnable(GL10.GL_DEPTH_TEST);
		
		mCamera.applyLookAt(mgl);
		
		onDrawFrame(mgl);
		onDrawFrameDone(mgl);
	}
	
	protected void onDrawFrame(MatrixTrackingGL gl) {
	}
	
	protected void onDrawFrameDone(MatrixTrackingGL gl) {
		if (mOnAfterNextRender != null) {
			mOnAfterNextRender.run(this, gl);
			mOnAfterNextRender = null;
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		gl.glViewport(0, 0, mWidth, mHeight);
    	mCamera.setScreenDimension(mWidth, mHeight);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		/*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);
        gl.glEnable(GL10.GL_DEPTH_TEST);
	}
	
	protected void clearScene(MatrixTrackingGL gl) {
		if (mClippingPlaneColor != null) {
    		 // define the color we want to be displayed as the "clipping wall"
			gl.glClearColor(mClippingPlaneColor.getRed(), 
							mClippingPlaneColor.getGreen(), 
							mClippingPlaneColor.getBlue(), 
							mClippingPlaneColor.getAlpha());
		}
		gl.glClearDepthf(1f);
		
        // clear the color buffer to show the ClearColor we called above...
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}
	
	public void setClippingPlaneColor(Color4f color) {
		mClippingPlaneColor = color;
	}
	
	public void setOnAfterNextRender(OnAfterNextRender run) {
		mOnAfterNextRender = run;
	}
	
	public Bitmap snapshot(MatrixTrackingGL gl) {
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
		bitmap.setPixels(data, size-width, -width, 0, 0, width, height);
		data = null;

		short sdata[] = new short[size];
		ShortBuffer sbuf = ShortBuffer.wrap(sdata);
		bitmap.copyPixelsToBuffer(sbuf);
		for (int i = 0; i < size; ++i) {
		    //BGR-565 to RGB-565
		    short v = sdata[i];
		    sdata[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
		}
		sbuf.rewind();
		bitmap.copyPixelsFromBuffer(sbuf);
		return bitmap;
	}
	
	public void snapshot(MatrixTrackingGL gl, File file) throws IOException {
		Bitmap bitmap = snapshot(gl);
		FileOutputStream fos = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		fos.flush();
		fos.close();
	}
	
}
