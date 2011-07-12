package com.tipsolutions.jacket.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class ControlSurfaceView extends GLSurfaceView implements IView {

//	final float TOUCH_SCALE_FACTOR = 180.0f / 320;
//	final float TRACKBALL_SCALE_FACTOR = 36.0f;
	IEventTap mEventTap = null;
//	final Controller mController;
	ControlRenderer mRenderer;
	
	public ControlSurfaceView(Context context) {
		super(context);
//		mController = new Controller(control, this);
	}

	public void setEventTap(IEventTap eventTap) {
		mEventTap = eventTap;
	}
	
	public Renderer getRenderer() {
		return mRenderer;
	}

	public void setRenderer(ControlRenderer renderer) {
		mRenderer = renderer;
		super.setRenderer(renderer);
	}

	@Override
	public void onResume() {
		super.onResume();
		requestRender();
	}
	
	@Override 
	public boolean onTouchEvent(final MotionEvent e) {
		boolean changed = false;
		if (mEventTap != null) {
    		float x = e.getX();
    		float y = e.getY();
    		switch (e.getAction()) {
        		case MotionEvent.ACTION_DOWN:
        			changed = mEventTap.pressDown(x, y);
        			break;
    	        case MotionEvent.ACTION_MOVE:
        			changed = mEventTap.pressMove(x, y);
        			break;
        		case MotionEvent.ACTION_UP:
        			changed = mEventTap.pressUp(x, y);
        			break;
    		}
    		if (changed) {
        		requestRender();
    		}
		}
		return changed;
	}
	
	
}
