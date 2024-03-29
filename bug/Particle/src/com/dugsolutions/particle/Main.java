package com.dugsolutions.particle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.dugsolutions.jacket.effect.Emitter;
import com.dugsolutions.jacket.effect.EmitterTex;
import com.dugsolutions.jacket.effect.ParticleSystem;
import com.dugsolutions.jacket.event.EventTapAdjust.Adjust;
import com.dugsolutions.jacket.event.EventTapTwirl;
import com.dugsolutions.jacket.event.EventTapTwirl.Rotate;
import com.dugsolutions.jacket.image.Texture;
import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.math.Bounds2D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.view.ControlRenderer;
import com.dugsolutions.jacket.view.ControlSurfaceView;
import com.dugsolutions.jacket.view.SpinnerControl;
import com.tipsolutions.particle.R;

public class Main extends Activity
{
	static final int		EMIT_DEFAULT	= 0;
	static final int		EMIT_TEXTURE	= 1;
	static final int		EMIT_FLARE		= 2;

	static final Color4f	DEFAULT_COLOR	= new Color4f(1f, 1f, 1f);
	static final Color4f	TEXTURE_COLOR	= new Color4f(0.9f, 0.9f, 0.9f);
	static final Color4f	FLARE_COLOR		= new Color4f(0.1f, 0.1f, 0.1f);

	static final float		VIEWING_DIST	= 1f;

	class MyRenderer extends ControlRenderer implements Adjust
	{
		Bounds2D	mPanBounds	= new Bounds2D();
		float		mMaxZ;

		public MyRenderer(ControlSurfaceView view, TextureManager tm)
		{
			super(view, tm);

			// mEventTap = new EventTapAdjust(this);
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height)
		{
			super.onSurfaceChanged(gl, width, height);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config)
		{
			super.onSurfaceCreated(gl, config);

			mParticleSystem.onCreate();

			float max = mParticleSystem.getMaxDistance();
			mPanBounds.setMinX(-max * 1.4f);
			mPanBounds.setMaxX(max * 1.4f);
			mPanBounds.setMinY(-max * 1.4f);
			mPanBounds.setMaxY(max * 1.4f);

			mCamera.setViewBounds(mParticleSystem.getBounds(), VIEWING_DIST);
			mMaxZ = mCamera.getViewingLoc().getZ();
		}

		@Override
		public void pan(float xDelta, float yDelta)
		{
			mCamera.pan(xDelta, yDelta, mPanBounds);
		}

		@Override
		public void scale(float delta)
		{
			mCamera.scale(delta, mMaxZ);
		}

		@Override
		protected void onDrawFrameContents(GL10 gl)
		{
			gl.glLoadIdentity();
			mCamera.applyViewBounds(gl);
			mParticleSystem.onDraw(gl);
		}
	};

	ControlSurfaceView	mSurfaceView;
	MyRenderer			mRenderer;
	ParticleSystem		mParticleSystem;
	MyApplication		mApp;
	EventTapTwirl		mTwirlEventTap;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		RelativeLayout main = new RelativeLayout(this);
		main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mApp = (MyApplication) getApplicationContext();

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(1);

		mRenderer = new MyRenderer(mSurfaceView, mApp.getTextureManager());
		mRenderer.setBackground(new Color4f(0.9f, 0.9f, 0.9f));

		mSurfaceView.setEGLConfigChooser(false);
		mSurfaceView.setRenderer(mRenderer);

		mTwirlEventTap = new EventTapTwirl(mSurfaceView, new Rotate()
		{
			@Override
			public void rotate(double xAngle, double yAngle)
			{
				mParticleSystem.addRotate(xAngle, yAngle, 0);
			}
		});
		// mTwirlEventTap.setDoubleTap(new Runnable()
		// {
		// @Override
		// public void run()
		// {
		// mSurfaceView.setEventTap(mCamera);
		// }
		// });
		// mCamera.setDoubleTap(new Runnable()
		// {
		// @Override
		// public void run()
		// {
		// mSurfaceView.setEventTap(mTwirlEventTap);
		// }
		// });

		mParticleSystem = new ParticleSystem(mSurfaceView);
		setEmitter(EMIT_DEFAULT);

		View controls = createControls();

		RelativeLayout.LayoutParams params;

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		main.addView(controls, params);

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ABOVE, controls.getId());
		main.addView(mSurfaceView, params);

		setContentView(main);
	}

	View createControls()
	{
		TableLayout holder = new TableLayout(this);
		holder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		holder.setStretchAllColumns(true);
		holder.setShrinkAllColumns(true);
		holder.setOrientation(TableLayout.HORIZONTAL);
		holder.setId(2);

		final Spinner emitChoice = new Spinner(this);
		ArrayAdapter<SpinnerControl> adapter = new ArrayAdapter<SpinnerControl>(this,
				android.R.layout.simple_spinner_item, new SpinnerControl[] {
						new SpinnerControl("Default", EMIT_DEFAULT), new SpinnerControl("Tex", EMIT_TEXTURE),
						new SpinnerControl("Flare", EMIT_FLARE), });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		emitChoice.setAdapter(adapter);
		emitChoice.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				SpinnerControl item = (SpinnerControl) emitChoice.getSelectedItem();
				setEmitter(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			}
		});
		emitChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		emitChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getEmitChoice()));

		final Spinner blendChoice = new Spinner(this);
		adapter = new ArrayAdapter<SpinnerControl>(this, android.R.layout.simple_spinner_item, new SpinnerControl[] {
				new SpinnerControl("Replace", GL10.GL_REPLACE), new SpinnerControl("Modulate", GL10.GL_MODULATE),
				new SpinnerControl("Decal", GL10.GL_DECAL), new SpinnerControl("Blend", GL10.GL_BLEND), });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		blendChoice.setAdapter(adapter);
		blendChoice.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				SpinnerControl item = (SpinnerControl) blendChoice.getSelectedItem();
				setBlendTexture(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			}

		});
		blendChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		blendChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getBlenderControl()));

		TableRow tableRow;
		tableRow = new TableRow(this);
		tableRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		tableRow.addView(emitChoice);
		tableRow.addView(blendChoice);
		holder.addView(tableRow);

		return holder;
	}

	void setEmitter(int code)
	{
		if (mApp.getEmitChoice() == code)
		{
			return;
		}
		switch (code)
		{
			case EMIT_FLARE:
			{
				mRenderer.setBackground(FLARE_COLOR);
				Texture tex = mApp.getTextureManager().getTexture(R.drawable.flaresmall);
				// tex.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
				EmitterTex emitter = new EmitterTex(tex);
				emitter.setSize(0.4f, 0.1f);
				emitter.setStartColor(new Color4f(1, 0, 0, 0));
				emitter.setEndColor(new Color4f(0, 1, 0, 0));
				emitter.setCreate(300, 75, 75);
				emitter.setAge(30, 1200 / 30, 1400 / 30);
				emitter.setStrength(0.02f, 0.02f);
				mParticleSystem.setEmitter(emitter);
				// mParticleSystem.DEBUG2 = true;
				break;
			}
			case EMIT_TEXTURE:
			{
				mRenderer.setBackground(TEXTURE_COLOR);
				Texture tex = mApp.getTextureManager().getTexture(R.drawable.flaresmall);
				tex.setBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
				EmitterTex emitter = new EmitterTex(tex);
				emitter.setCreate(300, 5, 15);
				emitter.setAge(100, 30, 50);
				emitter.setStartColor(Color4f.GREEN);
				emitter.setEndColor(Color4f.BLACK);
				emitter.setStrength(0.04f, 0.06f);
				emitter.setForce(new Vector3f(0, -0.02f, 0f));
				mParticleSystem.setEmitter(emitter);
				break;
			}
			case EMIT_DEFAULT:
			default:
			{
				mRenderer.setBackground(DEFAULT_COLOR);
				mParticleSystem.setEmitter(new Emitter());
				break;
			}
		}
		mApp.setEmitChoice(code);
		//
		// mCamera.setLookAt(mParticleSystem.getMatrix().getLocation());
		// mCamera.setLocation(mCamera.getLookAt().dup());
		// mCamera.getLocation().add(0, 0, mParticleSystem.getMaxDistance());
		//
		// Log.d("DEBUG", "Camera: " + mCamera.getLocation().toString());
		//
		// mSurfaceView.setEventTap(mCamera);
		mSurfaceView.requestRender();
	}

	void setBlendTexture(int param)
	{
		if (mApp.getBlenderControl() != param)
		{
			TextureManager tm = mApp.getTextureManager();
			mApp.setBlenderControl(param);
			tm.setBlendParam(param);
			mSurfaceView.requestRender();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mParticleSystem.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mParticleSystem.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_S)
		{
			// mRenderer.snapshot("/sdcard/screenshot.png");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}