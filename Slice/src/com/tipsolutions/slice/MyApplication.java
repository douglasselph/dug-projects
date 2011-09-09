package com.tipsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import android.app.Application;

import com.tipsolutions.jacket.image.TextureManager;

public class MyApplication extends Application {

	static final String TAG = "Slice";
	
    int mEglDepth = TestObj.EGL_NONE;
    int mBlenderControl = GL10.GL_MODULATE;
    int mActiveShapeIndex = DataManager.DATA_PYRAMID;
    
    DataManager mDataManager;
    TextureManager mTM;
    
//    Shape.CullFace mCullFace = Shape.CullFace.BACK;
    
    
    public int getActiveShapeIndex() { return mActiveShapeIndex; }
	public int getBlenderControl() { return mBlenderControl; }
    public DataManager getDataManager() { return mDataManager; }
    public TextureManager getTextureManager() { return mTM; }
    public int getEGLDepth() { return mEglDepth; }
    
    public void setActiveShapeIndex(int shape) { mActiveShapeIndex = shape; }
    public void setBlenderControl(int param) { mBlenderControl = param; }
    public void setEGLDepth(int param) { mEglDepth = param; }
    
//    public Shape.CullFace getCullFace() {
//    	return mCullFace;
//    }
//    
//    public void setCullFace(Shape.CullFace face) {
//    	mCullFace = face;
//    }
    
    @Override
	public void onCreate() {
		super.onCreate();
		
		mDataManager = new DataManager(this);
		mTM = new TextureManager(this);
	}
}
