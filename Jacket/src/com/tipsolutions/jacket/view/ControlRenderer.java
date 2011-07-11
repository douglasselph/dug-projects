package com.tipsolutions.jacket.view;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.tipsolutions.jacket.math.Color4f;

public class ControlRenderer implements GLSurfaceView.Renderer {
	
	protected final ControlSurfaceView mView;
	protected Color4f mClippingPlaneColor = null;
	
	public ControlRenderer(ControlSurfaceView view) {
		mView = view;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		reset(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	protected void reset(GL10 gl) {
		clearScene(gl);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
        gl.glFrontFace(GL10.GL_CCW); // Defines front face
        
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glCullFace(GL10.GL_BACK); // Do not draw this face
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
