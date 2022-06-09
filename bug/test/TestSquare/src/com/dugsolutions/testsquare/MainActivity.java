package com.dugsolutions.testsquare;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.dugsolutions.jacket.image.TextureManager;

public class MainActivity extends Activity
{
	GLSurfaceView	mSurfaceView;
	TextureManager	mTM;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mTM = new TextureManager(this);

		setContentView(R.layout.main);

		mSurfaceView = new GLSurfaceView(this);
		mSurfaceView.setRenderer(new RenderSquare(this, mTM));

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
