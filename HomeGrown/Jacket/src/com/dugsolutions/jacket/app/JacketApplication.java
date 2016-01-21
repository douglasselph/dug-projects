package com.dugsolutions.jacket.app;

import android.content.Context;

import com.dugsolutions.jacket.image.TextureManager;

public class JacketApplication
{
	final Context	mCtx;
	TextureManager	mTM;

	public JacketApplication(Context context)
	{
		mCtx = context;
	}

	public TextureManager getTM()
	{
		if (mTM == null)
		{
			mTM = new TextureManager(mCtx);
		}
		return mTM;
	}
}
