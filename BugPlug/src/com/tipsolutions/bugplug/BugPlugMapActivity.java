package com.tipsolutions.bugplug;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.tipsolutions.bugplug.map.RenderMap;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.MaterialColors;
import com.tipsolutions.jacket.math.Vector4f;
import com.tipsolutions.jacket.view.ColorControls;
import com.tipsolutions.jacket.view.ColorControls.OnOperation;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends SherlockActivity
{
	class ColorOp implements OnOperation
	{
		MaterialColors	mGlobalCopy;
		MaterialColors	mSpotCopy;

		MaterialColors copy(final MaterialColors src)
		{
			MaterialColors copy = new MaterialColors();
			copy.setAmbient(src.getAmbient());
			copy.setDiffuse(src.getDiffuse());
			copy.setEmission(src.getEmission());
			copy.setSpecular(src.getSpecular());
			return copy;
		}

		@Override
		public MaterialColors getMatColor(int what)
		{
			switch (what)
			{
				case WHAT_GLOBAL:
					if (mGlobalCopy == null)
					{
						mGlobalCopy = copy(mRenderMap.getGlobalMatColors());
					}
					pullGlobalPos();
					return mGlobalCopy;
				case WHAT_GROUND:
					return mRenderMap.getGroundMatColors();
				case WHAT_WATER:
					return mRenderMap.getWaterMatColors();
				case WHAT_SPOT:
					if (mSpotCopy == null)
					{
						mSpotCopy = copy(mRenderMap.getSpotMatColors());
					}
					pullSpotPos();
					return mSpotCopy;
			}
			return null;
		}

		void pullGlobalPos()
		{
			Vector4f pos = mRenderMap.getGlobalPos();
			Bounds2D bounds = mRenderMap.getBounds();
			float value = ((pos.getX() - bounds.getMinX()) / bounds.getSizeX()) * 127f;
			mGlobalCopy.setShininess(value);
		}

		void pullSpotPos()
		{
			Vector4f pos = mRenderMap.getSpotPos();
			Bounds2D bounds = mRenderMap.getBounds();
			float value = ((pos.getX() - bounds.getMinX()) / bounds.getSizeX()) * 127f;
			mSpotCopy.setShininess(value);
		}

		void pushGlobalPos()
		{
			Vector4f pos = mRenderMap.getGlobalPos();
			Bounds2D bounds = mRenderMap.getBounds();
			float value = mGlobalCopy.getShininess();
			pos.setX(value / 127f * bounds.getSizeX() + bounds.getMinX());
		}

		void pushSpotPos()
		{
			Vector4f pos = mRenderMap.getSpotPos();
			Bounds2D bounds = mRenderMap.getBounds();
			float value = mSpotCopy.getShininess();
			pos.setX(value / 127f * bounds.getSizeX() + bounds.getMinX());
		}

		@Override
		public void valueChanged(int what, MaterialColors value)
		{
			if (what == WHAT_SPOT)
			{
				pushSpotPos();
				mRenderMap.updateGlobalLights();
			}
			else if (what == WHAT_GLOBAL)
			{
				pushGlobalPos();
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
	FrameLayout			mBottom;
	ColorControls		mColorControls;

	int getTiltFactor()
	{
		int unit = mRenderMap.getTilt();
		if (unit < 0 || unit > MAX_TILT)
		{
			unit = 0;
		}
		return unit;
	}

	int getTiltIcon()
	{
		switch (getTiltFactor())
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
		int factor = getTiltFactor();

		if (factor == 0)
		{
			return getString(R.string.flat);
		}
		else
		{
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(getString(R.string.tilt));
			sbuf.append(factor);
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
					int factor = getTiltFactor();
					if (++factor > MAX_TILT)
					{
						factor = 0;
					}
					mRenderMap.setTilt(factor);

					item.setTitle(getTiltTitle());
					item.setIcon(getTiltIcon());
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
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSurfaceView.onResume();
	}

}