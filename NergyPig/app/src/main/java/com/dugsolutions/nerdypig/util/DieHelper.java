package com.dugsolutions.nerdypig.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GlobalInt;
import com.dugsolutions.nerdypig.util.SoundHelper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dug on 12/20/16.
 */

public class DieHelper
{
	public interface OnFinished
	{
		void onFinished(int value);
	}

	ImageView			mPicture;
	SoundHelper			mSound;
	Random				mRandom;
	OnFinished			mListener;

	public DieHelper(Context ctx, ImageView picture, OnFinished listener)
	{
		mSound = new SoundHelper(ctx);
		mPicture = picture;
		mRandom = new Random(System.currentTimeMillis());
		mListener = listener;
	}

	public void roll()
	{
		int value = mRandom.nextInt(6) + 1;

		setPicture(value);

		if (GlobalInt.hasAudio())
		{
			mSound.play();
		}
		if (mListener != null)
		{
			mListener.onFinished(value);
		}
	}

	public void setPicture(int face)
	{
		switch (face)
		{
			case 0:
				mPicture.setImageResource(R.drawable.dice3droll);
				break;
			case 1:
				mPicture.setImageResource(R.drawable.one);
				break;
			case 2:
				mPicture.setImageResource(R.drawable.two);
				break;
			case 3:
				mPicture.setImageResource(R.drawable.three);
				break;
			case 4:
				mPicture.setImageResource(R.drawable.four);
				break;
			case 5:
				mPicture.setImageResource(R.drawable.five);
				break;
			case 6:
				mPicture.setImageResource(R.drawable.six);
				break;
		}
	}

}
