package com.tipsolutions.bugplug;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.tipsolutions.bugplug.map.RenderMap;
import com.tipsolutions.jacket.math.MaterialColors;
import com.tipsolutions.jacket.view.ColorControls;
import com.tipsolutions.jacket.view.ColorControls.OnOperation;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends SherlockActivity
{
	class ColorOp implements OnOperation
	{
		@Override
		public MaterialColors getMatColor(int what)
		{
			switch (what)
			{
				case WHAT_GLOBAL:
					return mRenderMap.getGlobalMatColors();
				case WHAT_GROUND:
					return mRenderMap.getGroundMatColors();
				case WHAT_WATER:
					return mRenderMap.getWaterMatColors();
				case WHAT_SPOT:
					return mRenderMap.getGlobalSpotMatColors();
			}
			return null;
		}

		@Override
		public int getValue(int what)
		{
			return 0;
		}

		@Override
		public boolean hasParts(int what)
		{
			return true;
		}

		@Override
		public void valueChanged(int what, int value)
		{
			if (what == WHAT_GLOBAL || what == WHAT_SPOT)
			{
				mRenderMap.updateGlobalLights();
			}
			mSurfaceView.requestRender();
		}

		@Override
		public void valueChanged(int what, MaterialColors value)
		{
			if (what == WHAT_GLOBAL || what == WHAT_SPOT)
			{
				mRenderMap.updateGlobalLights();
			}
			mSurfaceView.requestRender();
		}

	};

	static final int	MAX_TILT	= 5;

	static final int	WHAT_GLOBAL	= 0;
	static final int	WHAT_GROUND	= 1;
	static final int	WHAT_WATER	= 2;
	static final int	WHAT_SPOT	= 3;

	RenderMap			mRenderMap;
	ControlSurfaceView	mSurfaceView;
	int					mTiltFactor	= 0;
	FrameLayout			mBottom;
	ColorControls		mColorControls;

	int getTiltIcon()
	{
		switch (mTiltFactor)
		{
			case 0:
				return R.drawable.tilt0;
			case 1:
				return R.drawable.tilt15;
			case 2:
				return R.drawable.tilt30;
			case 3:
				return R.drawable.tilt45;
			case 4:
				return R.drawable.tilt60;
			case 5:
				return R.drawable.tilt75;
		}
		return R.drawable.icon;

	}

	String getTiltTitle()
	{
		if (mTiltFactor == 0)
		{
			return getString(R.string.flat);
		}
		else
		{
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(getString(R.string.tilt));
			sbuf.append(mTiltFactor);
			return sbuf.toString();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		mRenderMap = new RenderMap(mSurfaceView, MyApplication.getTM(this));
		mSurfaceView.setRenderer(mRenderMap);
		mRenderMap.setRenderOnDirty();
		mSurfaceView.requestRender();

		FrameLayout container = (FrameLayout) findViewById(R.id.container);

		container.addView(mSurfaceView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		mBottom = (FrameLayout) findViewById(R.id.bottom);
		mColorControls = (ColorControls) findViewById(R.id.color_controls);
		mColorControls.setOpListener(new ColorOp());
		mColorControls.addWhat(R.drawable.dirt);
		mColorControls.addWhat(R.drawable.water);
		mColorControls.addWhat(R.drawable.spotlight);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.menu, menu);

		return true;
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
				if (mRenderMap != null)
				{
					if (++mTiltFactor > MAX_TILT)
					{
						mTiltFactor = 0;
					}
					item.setTitle(getTiltTitle());
					item.setIcon(getTiltIcon());

					mRenderMap.setTilt(mTiltFactor);
				}
				break;
			case R.id.menu_pan:
				if (mRenderMap != null)
				{
					mRenderMap.setIsPan(!mRenderMap.isPan());
					if (mRenderMap.isPan())
					{
						item.setTitle(R.string.pan);
						item.setIcon(R.drawable.pan);
					}
					else
					{
						item.setTitle(R.string.rotate);
						item.setIcon(R.drawable.rotate);
					}
				}
				break;
			case R.id.menu_reset:
				if (mRenderMap != null)
				{
					mRenderMap.resetView();
					mTiltFactor = 0;
					invalidateOptionsMenu();
				}
				break;
			case R.id.menu_controls:
				if (mBottom.getVisibility() == View.VISIBLE)
				{
					mBottom.setVisibility(View.GONE);
				}
				else
				{
					mBottom.setVisibility(View.VISIBLE);
					mColorControls.update();
				}
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSurfaceView.onResume();
	}

}