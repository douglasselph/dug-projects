package com.tipsolutions.jacket.view;

import android.util.FloatMath;
import android.view.MotionEvent;

/**
 * 
 * @author dug
 * 
 */
public class EventTapAdjust implements IEventTap
{
	public interface Adjust
	{
		void pan(int xDelta, int yDelta);

		void scale(int xDelta, int yDelta);
	};

	static final float	THRESHOLD	= 6f;

	protected Adjust	mAdjust;
	protected float		mX;
	protected float		mY;
	protected int		mUpdateResolution;
	protected int		mLastCallX;
	protected int		mLastCallY;

	public EventTapAdjust(Adjust adjust)
	{
		mAdjust = adjust;
		mUpdateResolution = 10;
	}

	public EventTapAdjust(Adjust adjust, int updateResolution)
	{
		mAdjust = adjust;
		mUpdateResolution = updateResolution;
	}

	public boolean onTouchEvent(MotionEvent ev)
	{
		int action = ev.getActionMasked();

		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
				mX = ev.getX();
				mY = ev.getY();
				return true;
			case MotionEvent.ACTION_UP:
				break;
			case MotionEvent.ACTION_MOVE:
			{
				if (ev.getPointerCount() > 1)
				{
					int xSpanDelta = (int) FloatMath.floor(Math.abs(ev.getX(1) - ev.getX(0)));
					int ySpanDelta = (int) FloatMath.floor(Math.abs(ev.getY(1) - ev.getY(0)));

					if (ySpanDelta > THRESHOLD || xSpanDelta > THRESHOLD)
					{
						if (checkResolution(xSpanDelta, ySpanDelta))
						{
							mAdjust.scale(xSpanDelta, ySpanDelta);
						}
						return true;
					}
				}
				int xDelta = (int) FloatMath.floor(Math.abs(ev.getX(0) - mX));
				int yDelta = (int) FloatMath.floor(Math.abs(ev.getY(0) - mY));

				if (xDelta > THRESHOLD && yDelta > THRESHOLD)
				{
					if (checkResolution(xDelta, yDelta))
					{
						mAdjust.pan(xDelta, yDelta);
					}
					return true;
				}
				break;
			}
		}
		return false;
	}

	boolean checkResolution(int xDelta, int yDelta)
	{
		if (mUpdateResolution == 0)
		{
			return true;
		}
		if (mLastCallX == 0 && mLastCallY == 0)
		{
			mLastCallX = xDelta;
			mLastCallY = yDelta;
			return true;
		}
		int callDeltaX = Math.abs(xDelta - mLastCallX);
		int callDeltaY = Math.abs(yDelta - mLastCallY);

		if (callDeltaX > mUpdateResolution || callDeltaY > mUpdateResolution)
		{
			mLastCallX = xDelta;
			mLastCallY = yDelta;
			return true;
		}
		return false;
	}

	/**
	 * Call the configured adjust callback only ever N pixels.
	 * 
	 * @param pixels
	 *        : last call has to differ by this much.
	 */
	public void setUpdateResolution(int updateResolution)
	{
		mUpdateResolution = updateResolution;
	}
}
