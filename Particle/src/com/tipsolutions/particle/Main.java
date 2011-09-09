package com.tipsolutions.particle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemSelectedListener;

import com.tipsolutions.jacket.effect.Emitter;
import com.tipsolutions.jacket.effect.EmitterTex;
import com.tipsolutions.jacket.effect.ParticleSystem;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.SpinnerControl;
import com.tipsolutions.jacket.view.TwirlEventTap;
import com.tipsolutions.jacket.view.TwirlEventTap.Rotate;

public class Main extends Activity {

	static final int EMIT_DEFAULT = 0;
	static final int EMIT_TEXTURE = 1;
	static final int EMIT_FLARE = 2;
	
	class MyRenderer extends ControlRenderer {
		public MyRenderer(ControlSurfaceView view, ControlCamera camera) {
			super(view, camera);
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			super.onDrawFrame(gl);
			
			mParticleSystem.onDraw(mGL);
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			super.onSurfaceChanged(gl, width, height);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			super.onSurfaceCreated(gl, config);
			
			getGL(gl);
			
			mParticleSystem.onCreate();
		}
		
	};
	
	ControlCamera mCamera;
	ControlSurfaceView mSurfaceView;
	MyRenderer mRenderer;
	ParticleSystem mParticleSystem;
	MyApplication mApp;
	TwirlEventTap mTwirlEventTap;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout main = new RelativeLayout(this);
        main.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        mApp = (MyApplication) getApplicationContext();
        mCamera = new ControlCamera();
    
        mSurfaceView = new ControlSurfaceView(this);
        mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mSurfaceView.setId(1);
        
        mRenderer = new MyRenderer(mSurfaceView, mCamera);
        mRenderer.setClippingPlaneColor(new Color4f(0.9f, 0.9f, 0.9f));
        
        mSurfaceView.setEGLConfigChooser(false);
        
        mSurfaceView.setRenderer(mRenderer);
       
        mTwirlEventTap = new TwirlEventTap(mSurfaceView, new Rotate() {
			@Override
			public void rotate(double xAngle, double yAngle) {
				mParticleSystem.addRotate(xAngle, yAngle, 0);
			}
        });
        mTwirlEventTap.setDoubleTap(new Runnable() {
			@Override
			public void run() {
				mSurfaceView.setEventTap(mCamera);
			}
        });
        mCamera.setDoubleTap(new Runnable() {
			@Override
			public void run() {
				mSurfaceView.setEventTap(mTwirlEventTap);
			}
        });
        
        mParticleSystem = new ParticleSystem(mSurfaceView);
        setEmitter(EMIT_DEFAULT);
        
        View controls = createControls();
      
        RelativeLayout.LayoutParams params;
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        
        main.addView(controls, params);
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ABOVE, controls.getId());
        main.addView(mSurfaceView, params);
    
        setContentView(main);
    }
    
    View createControls() {
    	
        TableLayout holder = new TableLayout(this);
        holder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        holder.setStretchAllColumns(true);
        holder.setShrinkAllColumns(true);
        holder.setOrientation(TableLayout.HORIZONTAL);
        holder.setId(2);
        
        final Spinner emitChoice = new Spinner(this);
        ArrayAdapter<SpinnerControl> adapter = new ArrayAdapter<SpinnerControl>(this,
        		android.R.layout.simple_spinner_item,
        		new SpinnerControl[] {
        		  new SpinnerControl("Default", EMIT_DEFAULT),
        		  new SpinnerControl("Tex", EMIT_TEXTURE),
        		  new SpinnerControl("Flare", EMIT_FLARE),
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emitChoice.setAdapter(adapter);
        emitChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				SpinnerControl item = (SpinnerControl) emitChoice.getSelectedItem();
				setEmitter(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        });
        emitChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        emitChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getEmitChoice()));
        
        final Spinner blendChoice = new Spinner(this);
        adapter = new ArrayAdapter<SpinnerControl>(this,
        		android.R.layout.simple_spinner_item,
        		new SpinnerControl[] {
        		  new SpinnerControl("Replace", GL10.GL_REPLACE),
        		  new SpinnerControl("Modulate", GL10.GL_MODULATE),
        		  new SpinnerControl("Decal", GL10.GL_DECAL),
        		  new SpinnerControl("Blend", GL10.GL_BLEND),
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        blendChoice.setAdapter(adapter);
        blendChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				SpinnerControl item = (SpinnerControl) blendChoice.getSelectedItem();
				setBlendTexture(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
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
    
    void setEmitter(int code) {
    	if (mApp.getEmitChoice() == code) {
    		return;
    	}
    	switch (code) {
    		case EMIT_FLARE:
    		{
    	        mRenderer.setClippingPlaneColor(new Color4f(0.1f, 0.1f, 0.1f));
    			Texture tex = mApp.getTextureManager().getTexture(R.drawable.flaresmall);
//    			tex.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
    			EmitterTex emitter = new EmitterTex(tex);
    			emitter.setSize(0.4f, 0.1f);
    			emitter.setStartColor(new Color4f(1, 0, 0, 0));
    			emitter.setEndColor(new Color4f(0, 1, 0, 0));
    			emitter.setCreate(300, 75, 75);
    			emitter.setAge(30, 1200/30, 1400/30);
    			emitter.setStrength(0.02f, 0.02f);
                mParticleSystem.setEmitter(emitter);
//    			mParticleSystem.DEBUG2 = true;
    			break;
    		}
    		case EMIT_TEXTURE:
    		{
    	        mRenderer.setClippingPlaneColor(new Color4f(0.9f, 0.9f, 0.9f));
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
    	        mRenderer.setClippingPlaneColor(new Color4f(0.9f, 0.9f, 0.9f));
                mParticleSystem.setEmitter(new Emitter());
                break;
    		}
    	}
    	mApp.setEmitChoice(code);
    	
    	mCamera.setLookAt(mParticleSystem.getMatrix().getLocation());
    	mCamera.setLocation(mCamera.getLookAt().dup());
    	mCamera.getLocation().add(0, 0, mParticleSystem.getMaxDistance());
    	
    	Log.d("DEBUG", "Camera: " + mCamera.getLocation().toString());
        
        mSurfaceView.setEventTap(mCamera);
        mSurfaceView.requestRender();
    }
    
	void setBlendTexture(int param) {
		if (mApp.getBlenderControl() != param) {
        	TextureManager tm = mApp.getTextureManager();
        	mApp.setBlenderControl(param);
        	tm.setBlendParam(param);
            mSurfaceView.requestRender();
		}
    }

	@Override
	protected void onPause() {
		super.onPause();
		mParticleSystem.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mParticleSystem.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_S) {
			mRenderer.snapshot("/sdcard/screenshot.png");
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}