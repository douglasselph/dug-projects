package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends Activity {

	class MyRenderer extends ControlRenderer {

		Map		mMap;
		Cube	mCube;

		// private float mAngle;

		public MyRenderer(ControlSurfaceView view) {
			super(view);
			mCube = new Cube();

			mMap = new Map(mTM);
		}

		@Override
		public void onDrawFrame(MatrixTrackingGL gl) {
			super.onDrawFrame(gl);
			mMap.onDraw(gl);

			// gl.glTranslatef(0, 0, -3.0f);
			// gl.glRotatef(mAngle, 0, 1, 0);
			// gl.glRotatef(mAngle * 0.25f, 1, 0, 0);
			//
			// gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			// gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			//
			// mCube.draw(gl);
			//
			// gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
			// gl.glTranslatef(0.5f, 0.5f, 0.5f);
			//
			// mCube.draw(gl);
			//
			// mAngle += 1.2f;
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

	static final int	SURFACE_ID	= 1;
	ControlSurfaceView	mSurfaceView;
	MyRenderer			mRenderer;
	TextView			mCamEye;
	TextView			mCamLook;
	TextView			mCamUp;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(SURFACE_ID);

		mRenderer = new MyRenderer(mSurfaceView);

		// EventTapAdjust eventTap = new EventTapAdjust(mSurfaceView,
		// new EventTapAdjust.Adjust() {
		// public void start(int x, int y) {
		// mState++;
		// }
		//
		// public void move(int xAmt, int yAmt) {
		// }
		//
		// });
		// mSurfaceView.setEventTap(eventTap);

		// mRenderer.setBackground(new Color4f(0.9f, 0.9f, 0.9f));0

		// mSurfaceView.setEGLConfigChooser(false);

		// CubeRenderer renderer = new CubeRenderer(false);
		mSurfaceView.setRenderer(mRenderer);

		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		container.addView(mSurfaceView, new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		// mSurfaceView.requestRender();

		mCamEye = (TextView) findViewById(R.id.cameraEye);
		mCamLook = (TextView) findViewById(R.id.cameraLook);
		mCamUp = (TextView) findViewById(R.id.cameraUp);

		setMessage();
	}

	void setMessage() {

	}
}