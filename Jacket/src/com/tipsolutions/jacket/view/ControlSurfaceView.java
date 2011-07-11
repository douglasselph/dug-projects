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
		if (mEventTap != null) {
    		float x = e.getX();
    		float y = e.getY();
    		switch (e.getAction()) {
        		case MotionEvent.ACTION_DOWN:
        			return mEventTap.pressDown(x, y);
    	        case MotionEvent.ACTION_MOVE:
        			return mEventTap.pressMove(x, y);
        		case MotionEvent.ACTION_UP:
        			return mEventTap.pressUp(x, y);
    		}
		}
		return false;
		
//		
//		float x = e.getX();
//		float y = e.getY();
//		switch (e.getAction()) {
//    		case MotionEvent.ACTION_DOWN:
//    			mController.pressDown(x, y);
//	            requestRender();
//    			break;
//	        case MotionEvent.ACTION_MOVE:
//    			mController.pressMove(x, y);
//	            requestRender();
//    			break;
//    		case MotionEvent.ACTION_UP:
//    			mController.pressUp(x, y);
//	            requestRender();
//    			break;
//		}
//		return true;
	}
}
