package com.tipsolutions.bugplug;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Box;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;

public class BugPlugMapActivity extends Activity {

	class MyRenderer extends ControlRenderer {

		Map		mMap;
		CubeF	mCube;
		Box		mBox;
		float	mAngle;

		public MyRenderer(ControlSurfaceView view) {
			super(view);
			mCube = new CubeF();
			mMap = new Map(mTM);
			// mBox = new Box(1f, 1f, 1f, mTM.getTexture(R.drawable.dirt));
			mBox = new Box(1f, 1f, 1f, Color4f.RED);
		}

		@Override
		public void onDrawFrame(MatrixTrackingGL gl) {
			super.onDrawFrame(gl);

			// mMap.onDraw(gl);
			drawCubes(gl);
		}

		void drawCubes(MatrixTrackingGL gl) {
			gl.glTranslatef(0, 0, -3.0f);

			gl.glRotatef(mAngle, 0, 1, 0);
			gl.glRotatef(mAngle * 0.25f, 1, 0, 0);

			mBox.onDraw(gl);

			gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
			gl.glTranslatef(0.5f, 0.5f, 0.5f);

			mBox.onDraw(gl);

			mAngle += 1.2f;
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

		EventTapAdjust eventTap = new EventTapAdjust(mSurfaceView,
				new EventTapAdjust.Adjust() {
					public void start(int x, int y) {
					}

					public void move(int xAmt, int yAmt) {
					}

				});
		mSurfaceView.setEventTap(eventTap);

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