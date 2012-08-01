package com.tipsolutions.bugplug;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tipsolutions.bugplug.map.Map;
import com.tipsolutions.bugplug.testobj.CubeF;
import com.tipsolutions.bugplug.testobj.TestSquare;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Box;
import com.tipsolutions.jacket.model.Square;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;

public class BugPlugMapActivity extends Activity
{

	class MyRenderer extends ControlRenderer
	{
		Map			mMap;
		CubeF		mCube;
		Box			mBox;
		TestSquare	mTestSquare;
		Square		mSquare;
		float		mAngle;

		public MyRenderer(ControlSurfaceView view)
		{
			super(view);
			mCube = new CubeF();
			mMap = new Map(MyApplication.getTM(getContext()));
			// mBox = new Box(1f, 1f, 1f, mTM.getTexture(R.drawable.dirt));
			mBox = new Box(1f);
			mBox.setColor(new Color4f(1f, 0f, 0f, 0.5f));
			mTestSquare = new TestSquare();
			mSquare = new Square(1);
			mSquare.setColor(Color4f.BLUE);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config)
		{
			super.onSurfaceCreated(gl, config);

			// mTestSquare.loadTexture(gl, getContext(), R.drawable.sample);
			// mSquare.setTexture(MyApplication.getTM(getContext()).getTexture(
			// R.drawable.sample));
			// mSquare.getTexture().load(gl);
		}

		@Override
		public void onDrawFrame(MatrixTrackingGL gl)
		{
			super.onDrawFrame(gl);

			// mMap.onDraw(gl);
			// drawCubes(gl);
			drawTestSquare(gl);
			// drawSquare(gl);
		}

		void drawCubes(MatrixTrackingGL gl)
		{
			gl.glTranslatef(0, 0, -3.0f);

			gl.glRotatef(mAngle, 0, 1, 0);
			gl.glRotatef(mAngle * 0.25f, 1, 0, 0);

			mBox.onDraw(gl);

			gl.glRotatef(mAngle * 2.0f, 0, 1, 1);
			gl.glTranslatef(0.5f, 0.5f, 0.5f);

			mBox.onDraw(gl);

			mAngle += 1.2f;
		}

		void drawTestSquare(MatrixTrackingGL gl)
		{
			gl.glTranslatef(0, 0, -3.0f);
			gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
			mTestSquare.draw(gl);
			mAngle += 1.2f;
		}

		void drawSquare(MatrixTrackingGL gl)
		{
			gl.glTranslatef(0, 0, -3.0f);
			gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
			mSquare.onDraw(gl);
			mAngle += 1.2f;
		}
	};

	class SimpleRenderer implements GLSurfaceView.Renderer
	{
		TestSquare	mSquare;
		float		mAngle;

		public SimpleRenderer()
		{
			mSquare = new TestSquare();
		}

		public void onDrawFrame(GL10 gl)
		{
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			gl.glLoadIdentity();

			gl.glTranslatef(0, 0, -5.0f);
			gl.glRotatef(mAngle * 0.25f, 0, 0, 1);
			mSquare.draw(gl);
			mAngle += 1.2f;
		}

		public void onSurfaceChanged(GL10 gl, int width, int height)
		{
			if (height == 0)
			{ // Prevent A Divide By Zero By
				height = 1; // Making Height Equal One
			}
			gl.glViewport(0, 0, width, height); // Reset The Current Viewport
			gl.glMatrixMode(GL10.GL_PROJECTION); // Select The Projection Matrix
			gl.glLoadIdentity(); // Reset The Projection Matrix

			// Calculate The Aspect Ratio Of The Window
			GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f,
					100.0f);

			gl.glMatrixMode(GL10.GL_MODELVIEW); // Select The Modelview Matrix
			gl.glLoadIdentity(); // Reset The Modelview Matrix
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config)
		{
			gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); // Black Background
			gl.glClearDepthf(1.0f); // Depth Buffer Setup

			mSquare.loadTexture(gl, getContext(), R.drawable.sample);
		}
	}

	static final int	SURFACE_ID	= 1;
	ControlSurfaceView	mSurfaceView;
	MyRenderer			mRenderer;
	SimpleRenderer		mSimpleRenderer;
	TextView			mCamEye;
	TextView			mCamLook;
	TextView			mCamUp;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(SURFACE_ID);

		mRenderer = new MyRenderer(mSurfaceView);
		mSimpleRenderer = new SimpleRenderer();

		EventTapAdjust eventTap = new EventTapAdjust(mSurfaceView,
				new EventTapAdjust.Adjust()
				{
					public void start(int x, int y)
					{
					}

					public void move(int xAmt, int yAmt)
					{
					}

				});
		mSurfaceView.setEventTap(eventTap);
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

	void setMessage()
	{

	}

	Context getContext()
	{
		return this;
	}
}