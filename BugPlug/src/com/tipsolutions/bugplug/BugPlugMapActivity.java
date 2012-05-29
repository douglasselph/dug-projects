package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends Activity {
	
	class MyRenderer extends ControlRenderer {
		
		Map mMap;
	    Cube mCube;
	    private float mAngle;
	    
		public MyRenderer(ControlSurfaceView view, ControlCamera camera) {
			super(view, camera);
	        mCube = new Cube();

//			mMap = new Map(mTM);
//			camera.lookAtFromAbove(mMap.getPrimaryModel());
//			Log.d("DEBUG", "Looking from " + camera.getLocation().toString() + ", on model with bounds=" + mMap.getPrimaryModel().getBounds().toString());
		}

		@Override
		public void onDrawFrame(MatrixTrackingGL gl) {
			super.onDrawFrame(gl);
//			mMap.onDraw(gl);
			
			/*
	         * Usually, the first thing one might want to do is to clear
	         * the screen. The most efficient way of doing this is to use
	         * glClear().
	         */

	        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

	        /*
	         * Now we're ready to draw some 3D objects
	         */
	        gl.glLoadIdentity();
	        gl.glTranslatef(0, 0, -3.0f);
	        gl.glRotatef(mAngle,        0, 1, 0);
	        gl.glRotatef(mAngle*0.25f,  1, 0, 0);

	        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

	        mCube.draw(gl);

	        gl.glRotatef(mAngle*2.0f, 0, 1, 1);
	        gl.glTranslatef(0.5f, 0.5f, 0.5f);

	        mCube.draw(gl);

	        mAngle += 1.2f;
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			super.onSurfaceChanged(gl, width, height);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			super.onSurfaceCreated(gl, config);
		}
		
	};
	ControlSurfaceView mSurfaceView;
	ControlCamera mCamera;
	MyRenderer mRenderer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		RelativeLayout main = new RelativeLayout(this);
	    main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	      
	    mSurfaceView = new ControlSurfaceView(this);
        mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mSurfaceView.setId(1);
        
        mCamera = new ControlCamera();
        
        mRenderer = new MyRenderer(mSurfaceView, mCamera);
//        mRenderer.setBackground(new Color4f(0.9f, 0.9f, 0.9f));
        
//        mSurfaceView.setEGLConfigChooser(false);
        
//        CubeRenderer renderer = new CubeRenderer(false); 
        mSurfaceView.setRenderer(mRenderer);
        
        RelativeLayout.LayoutParams params;

        params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        main.addView(mSurfaceView, params);
        
		setContentView(main);
		
//        mSurfaceView.requestRender();
	}
	
}