package com.tipsolutions.slice;

import java.io.InputStream;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {
	
    GLSurfaceView mGLSurfaceView;
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
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(new MyRenderer(figure, false));
        setContentView(mGLSurfaceView);
    }
    
    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
    }
}