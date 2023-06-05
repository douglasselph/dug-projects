package com.dugsolutions.nerdypig.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.act.TestRollDice2;
import com.dugsolutions.nerdypig.db.GlobalInt;

import java.util.Random;

/**
 * Created by dug on 12/20/16.
 */

public class DieHelper
{
	public interface OnFinished
	{
		void onFinished(int value);
	}

	static final int	ROLL_START	= 0;
	static final int	ROLL_FAST	= 1;
	static final int	ROLL_SLOW	= 2;
	static final int	FAST_MS		= 50;
	static final int	SLOW_MS		= 150;

	class DiceRollHandler extends Handler
	{
		final int	mInitialTime;
		final int	mEndingTime;
		int			mTimeLeft;

		DiceRollHandler(int initialTime, int endingTime)
		{
			mInitialTime = initialTime;
			mEndingTime = endingTime;
			mTimeLeft = mInitialTime + mEndingTime;
		}

		/**
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		public void handleMessage(Message msg)
		{
			int value = mRandom.nextInt(6) + 1;

			setPicture(value);

			int nextTime;
			int nextWhat;

			if (msg.what == ROLL_START)
			{
				nextTime = FAST_MS;
				nextWhat = ROLL_FAST;
			}
			else if (msg.what == ROLL_FAST)
			{
				if (mTimeLeft > mEndingTime)
				{
					nextWhat = ROLL_FAST;
					nextTime = FAST_MS;
				}
				else
				{
					nextWhat = ROLL_SLOW;
					nextTime = SLOW_MS;
				}
			}
			else
			{
				if (mTimeLeft > 0)
				{
					nextWhat = ROLL_SLOW;
					nextTime = SLOW_MS;
				}
				else
				{
					if (mListener != null)
					{
						mListener.onFinished(value);
					}
					return;
				}
			}
			mTimeLeft -= nextTime;
			mDiceHandler.sendMessageDelayed(Message.obtain(mDiceHandler, nextWhat), nextTime);
		}
	}

	ImageView		mPicture;
	SoundHelper		mSound;
	Random			mRandom;
	OnFinished		mListener;
	DiceRollHandler	mDiceHandler;

	public DieHelper(Context ctx, ImageView picture, OnFinished listener)
	{
		mSound = new SoundHelper(ctx);
		mPicture = picture;
		mRandom = new Random(System.currentTimeMillis());
		mListener = listener;
	}

	public void roll()
	{
		if (GlobalInt.hasAudio())
		{
			mSound.play();
		}
		final int dieWidth = mPicture.getWidth();
		final int dieHeight = mPicture.getHeight();

		View parent = (View) mPicture.getParent();
		final int parentWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
		final int parentHeight = parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
		float distToEdge = parentWidth - mPicture.getX() - dieWidth;
		float unitsToEdge = distToEdge / (float) dieWidth;
		final int initialTime = 500;
		final int endingTime = 650;

		float yRandom = mRandom.nextFloat();
		if (yRandom >= 0.5)
		{
			yRandom = 1;
		}
		else
		{
			yRandom = 0;
		}
		float distInPixels = parentHeight * yRandom - parentHeight / 2;
		float verticalUnits = distInPixels / (float) dieHeight / 4;

		AnimationSet set = new AnimationSet(true);
		set.setFillAfter(true);

		TranslateAnimation tanim = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, unitsToEdge, TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, verticalUnits);
		tanim.setDuration(initialTime);
		set.addAnimation(tanim);
		TranslateAnimation tanim2 = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0f,
				TranslateAnimation.RELATIVE_TO_SELF, -unitsToEdge / 2, TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, verticalUnits);
		tanim2.setStartOffset(initialTime);
		tanim2.setDuration(endingTime);
		tanim2.setInterpolator(new DecelerateInterpolator());
		set.addAnimation(tanim2);

		Log.d("MYDEBUG", "ANIMATION START!");
		
		mPicture.startAnimation(set);

		mDiceHandler = new DiceRollHandler(initialTime, endingTime);
		mDiceHandler.sendMessageDelayed(Message.obtain(mDiceHandler, ROLL_START), 0);
	}

	public void setPicture(int face)
	{
		mPicture.setImageLevel(face);
	}

	public void reset()
	{
		setPicture(0);
		mPicture.clearAnimation();
	}

}
