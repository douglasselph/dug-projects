package com.dugsolutions.nerdypig.util;

import android.content.Context;
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
	class MyHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			int value = mRandom.nextInt(6) + 1;

			if (--mRollCount > 0)
			{
				next();
			}
			else
			{
				setPicture(value);

				pause();

				if (mListener != null)
				{
					mListener.onFinished(value);
				}
			}
		}
	}

	public interface OnFinished
	{
		void onFinished(int value);
	}

	static final int	ROLL_DELAY		= 1;
	static final int	DEFAULT_COUNT	= 5;

	MyHandler			mHandler		= new MyHandler();
	ImageView			mPicture;
	SoundHelper			mSound;
	int					mRollCount;
	Timer				mTimer			= new Timer();
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
		roll(DEFAULT_COUNT);
	}

	public void roll(int count)
	{
		if (mRollCount <= 0)
		{
			mPicture.setImageResource(R.drawable.dice3droll);

			if (GlobalInt.hasAudio())
			{
				mSound.play();
			}
			next();
		}
	}

	void next()
	{
		mTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				mHandler.sendEmptyMessage(0);
			}
		}, ROLL_DELAY);
	}

	void pause()
	{
		if (GlobalInt.hasAudio())
		{
			mSound.pause();
		}
		mTimer.cancel();
		mTimer = new Timer();
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
