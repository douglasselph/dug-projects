package com.dugsolutions.jacket.event;

import com.dugsolutions.jacket.view.ControlSurfaceView;

/**
 * An event processor that twirls the associated view with mouse movement.
 * 
 * @author dug
 */
public class EventTapTwirl extends EventTap
{
	public interface Rotate
	{
		void rotate(double xAngle, double yAngle);
	};

	protected Rotate	mRotate;

	public EventTapTwirl(ControlSurfaceView view, Rotate rotate)
	{
		super(view);
		mRotate = rotate;
	}

	@Override
	public boolean pressMove(final float x, final float y)
	{
		mView.queueEvent(new Runnable()
		{
			public void run()
			{
				float xdiff = (mStartX - x);
				float ydiff = (mStartY - y);
				double yAngle = Math.toRadians(xdiff);
				double xAngle = Math.toRadians(ydiff);
				mRotate.rotate(xAngle, yAngle);
				mView.requestRender();
				mStartX = x;
				mStartY = y;
			}
		});
		return true;
	}
}
