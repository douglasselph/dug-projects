package com.tipsolutions.bugplug;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tipsolutions.bugplug.map.RenderMap;
import com.tipsolutions.bugplug.test.CubeFRenderer;
import com.tipsolutions.bugplug.test.CubeRenderer;
import com.tipsolutions.bugplug.test.RenderBox;
import com.tipsolutions.bugplug.test.RenderSquare;
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
	Renderer			mChoice		= Renderer.Map;

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
			return new RenderMap(mSurfaceView, MyApplication.getTM(this));
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

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(SURFACE_ID);

		EventTapAdjust eventTap = new EventTapAdjust(mSurfaceView, new EventTapAdjust.Adjust()
		{
			public void move(int xAmt, int yAmt)
			{
			}

			public void start(int x, int y)
			{
			}

		});
		mSurfaceView.setEventTap(eventTap);

		setRenderer(mChoice);

		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		container.addView(mSurfaceView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		// mSurfaceView.requestRender();

		mCamEye = (TextView) findViewById(R.id.cameraEye);
		mCamLook = (TextView) findViewById(R.id.cameraLook);
		mCamUp = (TextView) findViewById(R.id.cameraUp);

		setMessage();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_box:
				setRenderer(Renderer.BoxTex);
				break;
			case R.id.menu_cube:
				setRenderer(Renderer.Cube);
				break;
			case R.id.menu_map:
				setRenderer(Renderer.Map);
				break;
			case R.id.menu_square:
				setRenderer(Renderer.SquareTex);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	void setRenderer(Renderer which)
	{
		mChoice = which;
		mRenderer = getRenderer();
		mSurfaceView.setRenderer(mRenderer);

		Log.d("DEBUG", mRenderer.toString());
	}

	void setMessage()
	{

	}
}