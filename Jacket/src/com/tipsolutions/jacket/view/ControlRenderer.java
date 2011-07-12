package com.tipsolutions.jacket.view;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.tipsolutions.jacket.math.Color4f;

public class ControlRenderer implements GLSurfaceView.Renderer {
	
	protected final ControlSurfaceView mView;
	protected Color4f mClippingPlaneColor = null;
	protected int mWidth;
	protected int mHeight;
	
	public ControlRenderer(ControlSurfaceView view) {
		mView = view;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		clearScene(gl);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		gl.glMatrixMode(GL10.GL_PROJECTION); // Modify projection matrix in the following commands:

		gl.glMatrixMode(GL10.GL_MODELVIEW);  // Modify the modelview matrix in the following commands:
		gl.glFrontFace(GL10.GL_CCW); // Defines front face
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_BACK); // Do not draw this face

		gl.glEnable(GL10.GL_DEPTH_TEST);
	}
	
	protected void clearScene(GL10 gl) {
		if (mClippingPlaneColor != null) {
    		 // define the color we want to be displayed as the "clipping wall"
			gl.glClearColor(mClippingPlaneColor.getRed(), 
							mClippingPlaneColor.getGreen(), 
							mClippingPlaneColor.getBlue(), 
							mClippingPlaneColor.getAlpha());
		}
        // clear the color buffer to show the ClearColor we called above...
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}
	
	public void setClippingPlaneColor(Color4f color) {
		mClippingPlaneColor = color;
	}
	
}
