package com.tipsolutions.slice;

import android.app.Application;

public class MyApplication extends Application {

	static final String TAG = "Slice";
	
    boolean mEglDepth = false;
    
    public boolean getEGLDepth() {
    	return mEglDepth;
    }
    
    public void setEGLDepth(boolean flag) {
    	mEglDepth = flag;
    }

}
