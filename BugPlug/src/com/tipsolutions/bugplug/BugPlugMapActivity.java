package com.tipsolutions.bugplug;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.tipsolutions.bugplug.map.RenderMap;
import com.tipsolutions.bugplug.test.CubeFRenderer;
import com.tipsolutions.bugplug.test.CubeRenderer;
import com.tipsolutions.bugplug.test.RenderBox;
import com.tipsolutions.bugplug.test.RenderSquare;
import com.tipsolutions.bugplug.test.SimpleTestSquareRenderer;
import com.tipsolutions.bugplug.test.TestSquareRenderer;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends SherlockActivity
{
	enum Renderer
	{
		Box, BoxTex, Cube, CubeF, Map, Square, SquareTex, TestSquare
	};

	static final int		SURFACE_ID				= 1;
	static final Boolean	SIMPLE_TEST				= false;

	static final int		MAX_TILT				= 5;

	ControlRenderer			mRenderer;
	RenderMap				mRenderMap;
	ControlSurfaceView		mSurfaceView;
	GLSurfaceView			mSimpleSurfaceView;
	final Renderer			mChoice					= Renderer.Map;
	final boolean			mRenderOnlyWhenDirty	= true;
	boolean					mIsPan					= true;
	int						mTiltFactor				= 0;

	ControlRenderer getRenderer()
	{
		// Box, Cube, CubeF, Map, Square, TestSquare

		if (mChoice == Renderer.Box)
		{
			return new RenderBox(mSurfaceView, null);
		}
		else if (mChoice == Renderer.BoxTex)
		{
			return new RenderBox(mSurfaceView, MyApplication.getTM(this));
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
			return mRenderMap = new RenderMap(mSurfaceView, MyApplication.getTM(this));
		}
		else if (mChoice == Renderer.Square)
		{
			return new RenderSquare(mSurfaceView, null);
		}
		else if (mChoice == Renderer.SquareTex)
		{
			return new RenderSquare(mSurfaceView, MyApplication.getTM(this));
		}
		else if (mChoice == Renderer.TestSquare)
		{
			return new TestSquareRenderer(mSurfaceView, MyApplication.getTM(this));
		}
		return null;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mSimpleSurfaceView = new GLSurfaceView(this);
		mSimpleSurfaceView.setRenderer(new SimpleTestSquareRenderer(this));

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(SURFACE_ID);

		setRenderer(mChoice, mRenderOnlyWhenDirty);

		FrameLayout container = (FrameLayout) findViewById(R.id.container);

		if (SIMPLE_TEST)
		{
			container.addView(mSimpleSurfaceView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
		}
		else
		{
			container.addView(mSurfaceView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
		}
		// mCamEye = (TextView) findViewById(R.id.cameraEye);
		// mCamLook = (TextView) findViewById(R.id.cameraLook);
		// mCamUp = (TextView) findViewById(R.id.cameraUp);

		setMessage();
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item)
	{
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_exit:
				finish();
				break;
			case R.id.menu_tilt:
				if (++mTiltFactor > MAX_TILT)
				{
					mTiltFactor = 0;
				}
				if (mTiltFactor == 0)
				{
					item.setTitle(R.string.flat);
				}
				else
				{
					StringBuffer sbuf = new StringBuffer();
					sbuf.append(getString(R.string.tilt));
					sbuf.append(mTiltFactor);
					item.setTitle(sbuf.toString());
				}
				mRenderMap.setTilt(mTiltFactor);
				break;
			case R.id.menu_rotate:
				mIsPan = !mIsPan;
				if (mIsPan)
				{
					item.setTitle(R.string.pan);
				}
				else
				{
					item.setTitle(R.string.rotate);
				}
				mRenderMap.setIsPan(mIsPan);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	void setRenderer(Renderer which, boolean onlyWhenDirty)
	{
		mRenderer = getRenderer();

		mSurfaceView.setRenderer(mRenderer);
		if (onlyWhenDirty)
		{
			mRenderer.setRenderOnDirty();
			mSurfaceView.requestRender();
		}
		// Log.d("DEBUG", mRenderer.toString());
	}

	void setMessage()
	{

	}
}