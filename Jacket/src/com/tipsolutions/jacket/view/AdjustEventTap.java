package com.tipsolutions.jacket.view;

public class AdjustEventTap extends EventTap {

	public interface Adjust {
		void start(int x, int y);
		void move(int xAmt, int yAmt);
	};
	
	protected Adjust mAdjust;
	protected float mX;
	protected float mY;
	
	public AdjustEventTap(ControlSurfaceView view, Adjust adjust) {
		super(view);
		mAdjust = adjust;
	}
	
	@Override
	public boolean pressDown(float x, float y) {
		super.pressDown(x, y);
		mX = mStartX;
		mY = mStartY;
		if (mAdjust != null) {
			mAdjust.start((int)x, (int)y);
		}
		return true;
	}

	public boolean pressMove(final float x, final float y) {
		mView.queueEvent(new Runnable() {
			public void run() {
				if (mAdjust != null) {
    				mAdjust.move((int)x, (int)y);
    				mView.requestRender();
				}
			}
		});
		return true;
	}
	
}
