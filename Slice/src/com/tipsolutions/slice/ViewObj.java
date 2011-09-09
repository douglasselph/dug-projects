package com.tipsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemSelectedListener;

import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.data.ShapeGL;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.view.ButtonGroup;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.IEventTap;
import com.tipsolutions.jacket.view.SpinnerControl;
import com.tipsolutions.jacket.view.TwirlEventTap;
import com.tipsolutions.jacket.view.ButtonGroup.OnClickChangedListener;
import com.tipsolutions.jacket.view.TwirlEventTap.Rotate;

public class ViewObj extends Activity {

	interface CreateShape {
		ShapeGL create();
	}
	
	class Controls {
		TableLayout mHolder;
		TableRow mControlRow;
		TableRow mDisplayRow;
		
		Controls() {
			mHolder = new TableLayout(ViewObj.this);
			mHolder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mHolder.setStretchAllColumns(true);
			mHolder.setShrinkAllColumns(true);
			mHolder.setOrientation(TableLayout.HORIZONTAL);
			mHolder.setId(2);
			
			final Spinner shapeChoice = new Spinner(ViewObj.this);
			ArrayAdapter<SpinnerControl> adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this,
					android.R.layout.simple_spinner_item,
					new SpinnerControl[] {
					new SpinnerControl("Pyramid", DataManager.DATA_PYRAMID),
					new SpinnerControl("Cube", DataManager.DATA_CUBE),
					new SpinnerControl("Box", DataManager.DATA_BOX),
					new SpinnerControl("Susan", DataManager.DATA_SUSAN),
					new SpinnerControl("Hank", DataManager.DATA_HANK),
					new SpinnerControl("Wing1", DataManager.DATA_WING1),
			});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			shapeChoice.setAdapter(adapter);
			shapeChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					SpinnerControl item = (SpinnerControl) shapeChoice.getSelectedItem();
					setShape(item.getArg());
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
			shapeChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			shapeChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getActiveShapeIndex()));
			
			final Spinner blendChoice = new Spinner(ViewObj.this);
			adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this,
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

			final Spinner eglChoice = new Spinner(ViewObj.this);
			adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this,
					android.R.layout.simple_spinner_item,
					new SpinnerControl[] {
					new SpinnerControl("No EGL", EGL_NONE),
					new SpinnerControl("EGL Depth", EGL_DEPTH),
					new SpinnerControl("EGL NoDep", EGL_NO_DEPTH),
			});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			eglChoice.setAdapter(adapter);
			eglChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					SpinnerControl item = (SpinnerControl) eglChoice.getSelectedItem();
					setEGLDepth(item.getArg());
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
			eglChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			eglChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getEGLDepth()));
			
			ButtonGroup controlGroup = new ButtonGroup(ViewObj.this);
			controlGroup.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

			Button btnObj = new Button(ViewObj.this);
			btnObj.setId(CONTROL_OBJECT);
			btnObj.setText("Obj");
			btnObj.setLayoutParams(getParams());
			controlGroup.addView(btnObj);
			
			Button btnCam = new Button(ViewObj.this);
			btnCam.setId(CONTROL_CAMERA);
			btnCam.setText("Cam");
			btnCam.setLayoutParams(getParams());
			controlGroup.addView(btnCam);
			
			Button btnArm = new Button(ViewObj.this);
			btnArm.setId(CONTROL_ARMATURE);
			btnArm.setText("Arm");
			btnArm.setLayoutParams(getParams());
			controlGroup.addView(btnArm);
			
			controlGroup.setChecked(mApp.getActiveControl());
			controlGroup.setOnClickChangedListener(new OnClickChangedListener() {
				@Override
				public void onClickChanged(View v) {
					mApp.setActiveControl(v.getId());
					setEventTap();
				}
			});
			
			mControlRow = new TableRow(ViewObj.this);
			mControlRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mControlRow.addView(shapeChoice);
			mControlRow.addView(controlGroup);
			
			mDisplayRow = new TableRow(ViewObj.this);
			mDisplayRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mDisplayRow.addView(eglChoice);
			mDisplayRow.addView(blendChoice);
			
			mHolder.addView(mDisplayRow);
			mHolder.addView(mControlRow);
		}
		
		LinearLayout.LayoutParams getParams() {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.weight = 1;
			return params;
		}
	};
	
    static final int CONTROL_OBJECT = 0;
    static final int CONTROL_CAMERA = 1;
    static final int CONTROL_ARMATURE = 2;

	static final int EGL_NONE = 0;
	static final int EGL_DEPTH = 1;
	static final int EGL_NO_DEPTH = 2;
	
	static final int MENU_QUIT = 0;
	static final int MENU_RESET = 1;
	
    public static final boolean LOG = true;
    public static final String TAG = "Slice";
    
    ShapeGL mActiveShape = null;
    TwirlEventTap mTwirlEventTap;
    ControlCamera mCamera;
    IEventTap mActiveEventTap;
    MyRenderer mRenderer;
    ControlSurfaceView mSurfaceView;
    MyApplication mApp;
    Controls mControls;
   
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mApp = (MyApplication) getApplicationContext();
        mApp.getDataManager().init();

        RelativeLayout main = new RelativeLayout(this);
        main.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
//        LinearLayout main = new LinearLayout(this);
//        main.setOrientation(LinearLayout.VERTICAL);
        
        mCamera = new ControlCamera();
        mActiveEventTap = mCamera;
        
        mSurfaceView = new ControlSurfaceView(this);
        mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mSurfaceView.setId(1);
      
        // We want an 8888 pixel format because that's required for
        // a translucent window.
        // And we want a depth buffer.
//        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        
        // Use a surface format with an Alpha channel:
//        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        
        mRenderer = new MyRenderer(mSurfaceView, null, mCamera, false);
        mRenderer.setClippingPlaneColor(new Color4f(0.5f, 1.0f, 1.0f));
        
        if (mApp.getEGLDepth() != EGL_NONE) {
        	if (mApp.getEGLDepth() == EGL_DEPTH) {
                mSurfaceView.setEGLConfigChooser(true);
        	} else {
                mSurfaceView.setEGLConfigChooser(false);
        	}
        }
        mSurfaceView.setRenderer(mRenderer);
        
        mTwirlEventTap = new TwirlEventTap(mSurfaceView, new Rotate() {
			@Override
			public void rotate(double xAngle, double yAngle) {
				mActiveShape.getMatrixMod().addRotate(xAngle, yAngle, 0.0);
			}
        });
//        mTwirlEventTap.setDoubleTap(new Runnable() {
//			@Override
//			public void run() {
//				mSurfaceView.setEventTap(mCamera);
//			}
//        });
//        mCamera.setDoubleTap(new Runnable() {
//			@Override
//			public void run() {
//				mSurfaceView.setEventTap(mTwirlEventTap);
//			}
//        });
        mControls = new Controls();
      
        RelativeLayout.LayoutParams params;
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        
        main.addView(mControls.mHolder, params);
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ABOVE, mControls.mHolder.getId());
        main.addView(mSurfaceView, params);
    
        setContentView(main);
		setShape(mApp.getDataManager().getShape(mApp.getActiveShapeIndex()));
        
//        testMatrixAddRotate();
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int order = 0;
		menu.add(0, MENU_QUIT, order++, "Quit");
		menu.add(0, MENU_RESET, order++, "Reset");
		return true;
	};
   
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case MENU_QUIT:
    			finish();
    			break;
    		case MENU_RESET:
				mActiveShape.resetRotate();
				mSurfaceView.requestRender();
				break;
    	}
		return super.onOptionsItemSelected(item);
	}
    
    void reload() {
    	Intent intent = getIntent();
    	overridePendingTransition(0, 0);
    	finish();

    	overridePendingTransition(0, 0);
    	startActivity(intent);
    }

	@Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
//		int eglDepthPos = 0;
//		String msg;
//		if (mApp.getEGLDepth() == null) {
//			msg = "Use EGL with Depth";
//		} else if (!mApp.getEGLDepth()) {
//			msg = "Use EGL without Depth";
//		} else {
//			msg = "Disable EGL";
//		}
//		menu.getItem(eglDepthPos).setTitle(msg);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }
	
	void setBlendTexture(int param) {
		if (mApp.getBlenderControl() != param) {
        	TextureManager tm = mApp.getTextureManager();
        	mApp.setBlenderControl(param);
        	tm.setBlendParam(param);
            mSurfaceView.requestRender();
		}
    }
	
	void setEGLDepth(int param) {
		if (mApp.getEGLDepth() != param) {
    		mApp.setEGLDepth(param);
    		reload();	
		}
	}
	
//	void setCullFace(int face) {
//		if (mApp.getCullFace().ordinal() != face) {
//			mApp.setCullFace(Shape.GetCullFaceFromOrdinal(face));
//			mActiveShape.setCullFace(mApp.getCullFace());
//            mSurfaceView.requestRender();
//		}
//	}
	

//	float assertEquals(String what, Rotate r1, Rotate r2) {
//		float diff = 0;
//		diff += assertEquals(what + " AngleX", r1.getAngleXDegrees(), r2.getAngleXDegrees());
//		diff += assertEquals(what + " AngleY", r1.getAngleYDegrees(), r2.getAngleYDegrees());
//		diff += assertEquals(what + " AngleZ", r1.getAngleZDegrees(), r2.getAngleZDegrees());
//		return diff;
//	}
	
	public void setShape(int arg) {
		if (mApp.getActiveShapeIndex() != arg) {
    		mApp.setActiveShapeIndex(arg);
    		setShape(mApp.getDataManager().getShape(arg));
		}
	}
	
	void setShape(ShapeGL shape) {
    	mActiveShape = shape;
        mRenderer.setShape(shape);
        
    	mCamera.setLookAt(mActiveShape.getMidPoint());
    	mCamera.setLocation(mCamera.getLookAt().dup());
    	
    	Shape.Bounds bounds = mActiveShape.getBounds();
    	float offsetZ = bounds.getSizeZ();
    	if (bounds.getSizeY() > bounds.getSizeX()) {
    		offsetZ += bounds.getSizeY();
    	} else {
    		offsetZ += bounds.getSizeX();
    	}
    	mCamera.getLocation().add(0, 0, offsetZ);
        
        mSurfaceView.setEventTap(mActiveEventTap);
        mSurfaceView.requestRender();
    }
	
	void setEventTap() {
		switch (mApp.getActiveControl()) {
			case CONTROL_CAMERA:
				mActiveEventTap = mCamera;
				break;
			case CONTROL_OBJECT:
				mActiveEventTap = mTwirlEventTap;
				break;
			case CONTROL_ARMATURE:
				break;
		}
        mSurfaceView.setEventTap(mActiveEventTap);
	}
	
}