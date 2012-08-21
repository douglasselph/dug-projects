package com.tipsolutions.bugplug;

import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.tipsolutions.bugplug.map.RenderMap;
import com.tipsolutions.jacket.view.ControlSurfaceView;

public class BugPlugMapActivity extends SherlockActivity
{
	static final int	MAX_TILT	= 5;

	RenderMap			mRenderMap;
	ControlSurfaceView	mSurfaceView;
	int					mTiltFactor	= 0;

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
				}
				break;
			case R.id.menu_pan:
				if (mRenderMap != null)
				{
					mRenderMap.setIsPan(!mRenderMap.isPan());
					if (mRenderMap.isPan())
					{
						item.setTitle(R.string.pan);
					}
					else
					{
						item.setTitle(R.string.rotate);
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
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	void setMessage()
	{

	}
}