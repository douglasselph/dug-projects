package com.dugsolutions.particle;

import javax.microedition.khronos.opengles.GL10;

import android.app.Application;

import com.dugsolutions.jacket.app.JacketApplication;
import com.dugsolutions.jacket.image.TextureManager;

public class MyApplication extends Application
{

	static final String	TAG				= "Particle";

	int					mBlenderControl	= GL10.GL_MODULATE;
	int					mEmitChoice		= -1;
	JacketApplication	mJA;

	public int getBlenderControl()
	{
		return mBlenderControl;
	}

	public int getEmitChoice()
	{
		return mEmitChoice;
	}

	public TextureManager getTextureManager()
	{
		if (mJA == null)
		{
			mJA = new JacketApplication(this);
		}
		return mJA.getTM();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	public void setBlenderControl(int param)
	{
		mBlenderControl = param;
	}

	public void setEmitChoice(int param)
	{
		mEmitChoice = param;
	}
}
