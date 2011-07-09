package com.tipsolutions.slice;

import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.tipsolutions.jacket.view.CameraControl;
import com.tipsolutions.view.ControlSurfaceView;

public class Main extends Activity {
	
	public static final boolean LOG = true;
	public static final String TAG = "Slice";
	
    ControlSurfaceView mSurfaceView;
    CameraControl mCamera;
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
        mCamera = new CameraControl();
        mSurfaceView = new ControlSurfaceView(this, mCamera);
        mSurfaceView.setRenderer(new MyRenderer(figure, mCamera, false));
        setContentView(mSurfaceView);
        mSurfaceView.requestFocus();
        mSurfaceView.setFocusableInTouchMode(true);
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