package com.dugsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import android.app.Application;

import com.dugsolutions.jacket.image.TextureManager;

public class MyApplication extends Application
{

	static final String	TAG					= "Slice";

	int					mEglDepth			= ViewObj.EGL_NONE;
	int					mBlenderControl		= GL10.GL_MODULATE;
	int					mActiveShapeIndex	= DataManager.DATA_PYRAMID;
	int					mActiveControl		= ViewObj.CONTROL_CAMERA;

	DataManager			mDataManager;
	TextureManager		mTM;

	// Shape.CullFace mCullFace = Shape.CullFace.BACK;

	public int getActiveShapeIndex()
	{
		return mActiveShapeIndex;
	}

	public int getBlenderControl()
	{
		return mBlenderControl;
	}

	public DataManager getDataManager()
	{
		return mDataManager;
	}

	public int getEGLDepth()
	{
		return mEglDepth;
	}

	public int getActiveControl()
	{
		return mActiveControl;
	}

	public void setActiveShapeIndex(int shape)
	{
		mActiveShapeIndex = shape;
	}

	public void setBlenderControl(int param)
	{
		mBlenderControl = param;
	}

	public void setEGLDepth(int param)
	{
		mEglDepth = param;
	}

	public void setActiveControl(int code)
	{
		mActiveControl = code;
	}

	// public Shape.CullFace getCullFace() {
	// return mCullFace;
	// }
	//
	// public void setCullFace(Shape.CullFace face) {
	// mCullFace = face;
	// }

	@Override
	public void onCreate()
	{
		super.onCreate();
		mDataManager = new DataManager(this);
		mTM = new TextureManager(this);
	}

	public TextureManager getTM()
	{
		return mTM;
	}
}
