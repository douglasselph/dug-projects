package com.tipsolutions.particle;

import javax.microedition.khronos.opengles.GL10;

import android.app.Application;

public class MyApplication extends Application {

	static final String TAG = "Particle";
	
    int mBlenderControl = GL10.GL_MODULATE;
    int mEmitChoice = -1;
    
    public int getBlenderControl() {
    	return mBlenderControl;
    }
    
    public void setBlenderControl(int param) {
    	mBlenderControl = param;
    }
    
    public int getEmitChoice() {
    	return mEmitChoice;
    }
    
    public void setEmitChoice(int param) {
    	mEmitChoice = param;
    }
}
