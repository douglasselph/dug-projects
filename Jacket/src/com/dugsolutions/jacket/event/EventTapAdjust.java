package com.dugsolutions.jacket.event;

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
		void pan(float xDelta, float yDelta);

		void scale(float delta);

		int getWidth();

		int getHeight();
	};

	static final float	THRESHOLD		= 6f;
	static final float	UPDATE_SCALE	= 0.1f;
	static final float	UPDATE_PAN		= 0.05f;

	static int LARGER(int v1, int v2)
	{
		return v1 >= v2 ? v1 : v2;
	}

	protected Adjust	mAdjust;
	protected float		mX;
	protected float		mY;
	protected int		mLastCallX;
	protected int		mLastCallY;
	protected float		mStartScaleDelta;
	protected float		mStartPanXDelta;
	protected float		mStartPanYDelta;

	public EventTapAdjust(Adjust adjust)
	{
		mAdjust = adjust;
	}

	public boolean onTouchEvent(MotionEvent ev)
	{
		int action = ev.getActionMasked();

		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
			{
				mX = ev.getX();
				mY = ev.getY();
				mStartScaleDelta = 0;
				mStartPanXDelta = 0;
				mStartPanYDelta = 0;
				return true;
			}
			case MotionEvent.ACTION_UP:
			{
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				if (ev.getPointerCount() > 1)
				{
					int xSpanDelta = (int) FloatMath.floor(Math.abs(ev.getX(1) - ev.getX(0)));
					int ySpanDelta = (int) FloatMath.floor(Math.abs(ev.getY(1) - ev.getY(0)));

					if (ySpanDelta > THRESHOLD || xSpanDelta > THRESHOLD)
					{
						float delta;

						if (ySpanDelta > xSpanDelta)
						{
							delta = (float) ySpanDelta / mAdjust.getHeight();
						}
						else
						{
							delta = (float) xSpanDelta / mAdjust.getWidth();
						}
						if (mStartScaleDelta == 0)
						{
							mStartScaleDelta = delta;
						}
						else
						{
							float diffDelta = delta - mStartScaleDelta;

							if (Math.abs(diffDelta) > UPDATE_SCALE)
							{
								mStartScaleDelta = delta;
								mAdjust.scale(1 - diffDelta);
							}
						}
						return true;
					}
				}
				float xDelta = ev.getX(0) - mX;
				float yDelta = ev.getY(0) - mY;

				if (Math.abs(xDelta) > THRESHOLD || Math.abs(yDelta) > THRESHOLD)
				{
					float panXDelta = (float) xDelta / mAdjust.getWidth();
					float panYDelta = (float) yDelta / mAdjust.getHeight();
					float diffXDelta;
					float diffYDelta;

					if (mStartPanXDelta == 0)
					{
						mStartPanXDelta = panXDelta;
						diffXDelta = 0;
					}
					else
					{
						diffXDelta = panXDelta - mStartPanXDelta;
					}
					if (mStartPanYDelta == 0)
					{
						mStartPanYDelta = panYDelta;
						diffYDelta = 0;
					}
					else
					{
						diffYDelta = panYDelta - mStartPanYDelta;
					}
					if (Math.abs(diffXDelta) > UPDATE_PAN || Math.abs(diffYDelta) > UPDATE_PAN)
					{
						mStartPanXDelta = panXDelta;
						mStartPanYDelta = panYDelta;
						mAdjust.pan(diffXDelta, diffYDelta);
					}
					return true;
				}
				break;
			}
		}
		return false;
	}
}
