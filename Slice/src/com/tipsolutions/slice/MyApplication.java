package com.tipsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import android.app.Application;

public class MyApplication extends Application {

	static final String TAG = "Slice";
	
    int mEglDepth = Main.EGL_NONE;
    int mBlenderControl = GL10.GL_MODULATE;
    int mActiveShapeIndex = Main.DATA_PYRAMID;
    
    public int getEGLDepth() {
    	return mEglDepth;
    }
    
    public void setEGLDepth(int param) {
    	mEglDepth = param;
    }
    
    public int getBlenderControl() {
    	return mBlenderControl;
    }
    
    public void setBlenderControl(int param) {
    	mBlenderControl = param;
    }
    
    public int getActiveShapeIndex() {
    	return mActiveShapeIndex;
    }
    
    public void setActiveShapeIndex(int shape) {
    	mActiveShapeIndex = shape;
    }
    
}
