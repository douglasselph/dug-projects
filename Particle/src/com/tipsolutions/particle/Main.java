package com.tipsolutions.particle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemSelectedListener;

import com.tipsolutions.jacket.effect.ParticleSystem;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.SpinnerControl;
import com.tipsolutions.jacket.view.TwirlEventTap;
import com.tipsolutions.jacket.view.TwirlEventTap.Rotate;

public class Main extends Activity {

	static final int EMIT_DEFAULT = 0;
	
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
        mRenderer.setClippingPlaneColor(new Color4f(0.5f, 1.0f, 1.0f));
        
        mSurfaceView.setRenderer(mRenderer);
       
        mTwirlEventTap = new TwirlEventTap(mSurfaceView, new Rotate() {
			@Override
			public void rotate(double xAngle, double yAngle) {
				mParticleSystem.getMatrix().addRotate(xAngle, yAngle, 0);
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
        
        final int frameIntervalMs = 30;
        mParticleSystem = new ParticleSystem(mSurfaceView, frameIntervalMs);
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
    	switch (code) {
    		case EMIT_DEFAULT:
    		default:
    			final int create = 30;
    			final int createVar = 5;
    			final int lifeMs = 3000;
    			final int maxAge = lifeMs/mParticleSystem.getFrameIntervalMs();
    			final int maxAgeVar = 10;
    			final float strength = 0.1f;
    			final float strengthVar = 0.03f;
    			
                mParticleSystem.setEmitter(mParticleSystem.new Emitter(
                		create, createVar, 
                		maxAge, maxAgeVar, 
                		strength, strengthVar));
                break;
    	}
    	mCamera.setLookAt(mParticleSystem.getMatrix().getLocation());
    	mCamera.setLocation(mCamera.getLookAt().dup());
    	mCamera.getLocation().add(0, 0, mParticleSystem.getMaxDistance());
        
        mSurfaceView.setEventTap(mCamera);
        mSurfaceView.requestRender();
    }
    
	void setBlendTexture(int param) {
		if (mApp.getBlenderControl() != param) {
        	TextureManager tm = mRenderer.getTextureManager();
        	mApp.setBlenderControl(param);
        	tm.setDefaultBlendParam(param);
            for (Texture t : tm.getTextures()) { 
            	t.setBlendParam(param);
            }
            mSurfaceView.requestRender();
		}
    }
}