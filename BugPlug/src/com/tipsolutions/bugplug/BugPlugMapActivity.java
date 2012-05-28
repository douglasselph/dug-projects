package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends Activity {
	
	class MyRenderer extends ControlRenderer {
		
		Map mMap;
		
		public MyRenderer(ControlSurfaceView view, ControlCamera camera) {
			super(view, camera);
			mMap = new Map(mTM);
			camera.lookAt(mMap.getPrimaryModel());
		}

		@Override
		public void onDrawFrame(MatrixTrackingGL gl) {
			mMap.onDraw(gl);
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
	    main.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	      
	    mSurfaceView = new ControlSurfaceView(this);
        mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mSurfaceView.setId(1);
        
        mCamera = new ControlCamera();
        
        mRenderer = new MyRenderer(mSurfaceView, mCamera);
        mRenderer.setBackground(new Color4f(0.9f, 0.9f, 0.9f));
        
        mSurfaceView.setEGLConfigChooser(false);

        RelativeLayout.LayoutParams params;

        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        main.addView(mSurfaceView, params);
        
		setContentView(main);
	}
	
}