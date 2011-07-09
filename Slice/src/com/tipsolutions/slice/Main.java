package com.tipsolutions.slice;

import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.tipsolutions.jacket.view.CameraControl;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.IEventTap;

public class Main extends Activity {
	
	public static final boolean LOG = true;
	public static final String TAG = "Slice";
	
    ControlSurfaceView mSurfaceView;
    CameraControl mCamera;
    MyRenderer mRenderer;
    final String FigureFile = "cube.data";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Figure figure = new Figure();
        try {
            InputStream inputStream = getAssets().open(FigureFile);
            figure.readData(inputStream);
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
//        mCamera = new CameraControl();
//        mSurfaceView = new ControlSurfaceView(this, mCamera);
        mSurfaceView = new ControlSurfaceView(this);
        
        mSurfaceView.setRenderer(mRenderer = new MyRenderer(figure, mCamera, false));
        mSurfaceView.setEventTap(new IEventTap() {
        	public boolean pressDown(final float x, final float y) {
        		mSurfaceView.queueEvent(new Runnable() {
        			public void run() {
        				mRenderer.setColor(x / mSurfaceView.getWidth(), 
        								   y / mSurfaceView.getHeight(), 1.0f);
        			}
        		});
        		return true;
        	}
        	
        	public boolean pressMove(float x, float y) {
        		return false;
        	}
        	public boolean pressUp(float x, float y){
        		return false;
        	}
        });
        setContentView(mSurfaceView);
//        mSurfaceView.requestFocus();
//        mSurfaceView.setFocusableInTouchMode(true);
    }
    
	
    
    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }
}