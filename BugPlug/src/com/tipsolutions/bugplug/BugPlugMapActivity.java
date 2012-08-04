package com.tipsolutions.bugplug;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tipsolutions.bugplug.test.CubeFRenderer;
import com.tipsolutions.bugplug.test.CubeRenderer;
import com.tipsolutions.bugplug.test.TestSquareRenderer;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;

public class BugPlugMapActivity extends Activity
{
	enum Renderer
	{
		Box, BoxTex, Cube, CubeF, Map, Square, SquareTex, TestSquare
	};

	static final int	SURFACE_ID	= 1;

	TextView			mCamEye;
	TextView			mCamLook;
	TextView			mCamUp;
	ControlRenderer		mRenderer;
	ControlSurfaceView	mSurfaceView;
	final Renderer		mChoice		= Renderer.BoxTex;

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

		EventTapAdjust eventTap = new EventTapAdjust(mSurfaceView,
				new EventTapAdjust.Adjust()
				{
					public void move(int xAmt, int yAmt)
					{
					}

					public void start(int x, int y)
					{
					}

				});
		mSurfaceView.setEventTap(eventTap);
		mSurfaceView.setRenderer(getRenderer());

		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		container.addView(mSurfaceView, new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		// mSurfaceView.requestRender();

		mCamEye = (TextView) findViewById(R.id.cameraEye);
		mCamLook = (TextView) findViewById(R.id.cameraLook);
		mCamUp = (TextView) findViewById(R.id.cameraUp);

		setMessage();
	}

	ControlRenderer getRenderer()
	{
		// Box, Cube, CubeF, Map, Square, TestSquare

		if (mChoice == Renderer.Box)
		{
			return new RenderBox(this, mSurfaceView, false);
		}
		else if (mChoice == Renderer.BoxTex)
		{
			return new RenderBox(this, mSurfaceView, true);
		}
		else if (mChoice == Renderer.Cube)
		{
			return new CubeRenderer(mSurfaceView);
		}
		else if (mChoice == Renderer.CubeF)
		{
			return new CubeFRenderer(mSurfaceView);
		}
		else if (mChoice == Renderer.Map)
		{
			return new RenderMap(this, mSurfaceView);
		}
		else if (mChoice == Renderer.Square)
		{
			return new RenderSquare(this, mSurfaceView, false);
		}
		else if (mChoice == Renderer.SquareTex)
		{
			return new RenderSquare(this, mSurfaceView, true);
		}
		else if (mChoice == Renderer.TestSquare)
		{
			return new TestSquareRenderer(this, mSurfaceView);
		}
		return null;
	}

	void setMessage()
	{

	}
}