package com.tipsolutions.slice;

import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.tipsolutions.jacket.view.Camera;
import com.tipsolutions.jacket.view.CameraControl;
import com.tipsolutions.view.ControlSurfaceView;

public class Main extends Activity {
	
    ControlSurfaceView mSurfaceView;
    CameraControl mCamera;
    final String HankFile = "hank.data";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Figure figure = new Figure();
        
        try {
            InputStream inputStream = getAssets().open(HankFile);
            figure.readData(inputStream);
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
        mCamera = new CameraControl();
        mSurfaceView = new ControlSurfaceView(this, mCamera);
        mSurfaceView.setRenderer(new MyRenderer(figure, mCamera, false));
        setContentView(mSurfaceView);
    }
    
    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mSurfaceView.onPause();
    }
}