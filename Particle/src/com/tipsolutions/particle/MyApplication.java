package com.tipsolutions.particle;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;

import android.app.Application;

public class MyApplication extends Application {

	static final String TAG = "Particle";
	
    int mBlenderControl = GL10.GL_MODULATE;
    int mEmitChoice = -1;
    TextureManager mTM;
    
    public int getBlenderControl() {
    	return mBlenderControl;
    }
    public int getEmitChoice() {
    	return mEmitChoice;
    }

	public TextureManager getTextureManager() { return mTM; }
    
    @Override
	public void onCreate() {
		super.onCreate();
		mTM = new TextureManager(this);
	}
    
    public void setBlenderControl(int param) {
    	mBlenderControl = param;
    }
    
    public void setEmitChoice(int param) {
    	mEmitChoice = param;
    }
}
