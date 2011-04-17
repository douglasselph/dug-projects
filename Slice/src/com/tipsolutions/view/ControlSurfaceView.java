package com.tipsolutions.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.tipsolutions.jacket.view.Controller;
import com.tipsolutions.jacket.view.IControl;
import com.tipsolutions.jacket.view.IView;

public class ControlSurfaceView extends GLSurfaceView implements IView {

	final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	final float TRACKBALL_SCALE_FACTOR = 36.0f;
	final Controller mController;
	
	public ControlSurfaceView(Context context, IControl control) {
		super(context);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mController = new Controller(control, this);
	}

//	@Override 
//	public boolean onTrackballEvent(MotionEvent e) {
//		requestRender();
//		return true;
//	}

	@Override 
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		switch (e.getAction()) {
    		case MotionEvent.ACTION_DOWN:
    			mController.pressDown(x, y);
    			break;
	        case MotionEvent.ACTION_MOVE:
    			mController.pressMove(x, y);
	            requestRender();
    			break;
    		case MotionEvent.ACTION_UP:
    			break;
		}
		return true;
	}
}
