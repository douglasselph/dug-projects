package com.tipsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.data.Shape;

import android.app.Application;

public class MyApplication extends Application {

	static final String TAG = "Slice";
	
    int mEglDepth = Main.EGL_NONE;
    int mBlenderControl = GL10.GL_MODULATE;
    int mActiveShapeIndex = Main.DATA_PYRAMID;
    Shape.CullFace mCullFace = Shape.CullFace.BACK;
    
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
    
    public Shape.CullFace getCullFace() {
    	return mCullFace;
    }
    
    public void setCullFace(Shape.CullFace face) {
    	mCullFace = face;
    }
    
    
}
