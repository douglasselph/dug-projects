package com.dugsolutions.testsquaremodel;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.view.ControlSurfaceView;

public class MainActivity extends Activity
{
	RenderModelSquare	mRenderer;
	ControlSurfaceView	mSurfaceView;
	TextureManager		mTM;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mTM = new TextureManager(this);

		setContentView(R.layout.main);

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		mRenderer = new RenderModelSquare(mSurfaceView, mTM);
		mSurfaceView.setRenderer(mRenderer);

		FrameLayout container = (FrameLayout) findViewById(R.id.container);

		container.addView(mSurfaceView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_exit:
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

}
