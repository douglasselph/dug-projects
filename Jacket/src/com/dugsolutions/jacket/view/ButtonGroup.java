package com.dugsolutions.jacket.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class ButtonGroup extends LinearLayout {
	
	protected class CheckedStateTracker implements OnClickListener {
		synchronized public void onClick(View v) {
	        if (mIgnore) {
	            return; // prevents from infinite recursion
	        }
	        mIgnore = true;
	        mCheckedId = v.getId();
	        if (mOnClickChangedListener != null) {
	        	mOnClickChangedListener.onClickChanged(v);
	        }
	        mHandler.sendEmptyMessageDelayed(MSG_SET_CHECKED, 100);
		}
	}
	
	public interface OnClickChangedListener {
		void onClickChanged(View v);
	}
	
	/**
	 * <p>A pass-through listener acts upon the events and dispatches them
	 * to another listener. This allows the table layout to set its own internal
	 * hierarchy change listener without preventing the user to setup his.</p>
	 */
	protected class PassThroughHierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener {
		
	    ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener = null;
	    /**
	     * {@inheritDoc}
	     */
	    public void onChildViewAdded(View parent, View child) {
	        if (parent == ButtonGroup.this) {
	        	if (child instanceof Button) {
	        		int id = child.getId();
	        		// generates an id if it's missing
	        		if (id == View.NO_ID) {
	        			id = child.hashCode();
	        			child.setId(id);
	        		}
	        		Button btn = (Button) child;
	        		btn.setOnClickListener(mOnChildClickListener);
	        	} 
	        }
	        if (mOnHierarchyChangeListener != null) {
	            mOnHierarchyChangeListener.onChildViewAdded(parent, child);
	        }
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void onChildViewRemoved(View parent, View child) {
	         if (parent == ButtonGroup.this && child instanceof Button) {
	             ((Button) child).setOnClickListener(null);
	        }
	        if (mOnHierarchyChangeListener != null) {
	            mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
	        }
	    }
	}
	
	final static int MSG_SET_CHECKED = 0;
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
    			case MSG_SET_CHECKED:
    				setChecked();
    				mIgnore = false;
    				break;
			}
		}
	};
	
	OnClickChangedListener mOnClickChangedListener = null;
	CheckedStateTracker mOnChildClickListener = new CheckedStateTracker();
	boolean mIgnore = false;
	int mCheckedId = -1;

	public ButtonGroup(Context context) {
		super(context);
		init();
	}

	public ButtonGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	void init() {
		super.setOnHierarchyChangeListener(new PassThroughHierarchyChangeListener());
	}
	
	public void setOnClickChangedListener(OnClickChangedListener listener) {
		mOnClickChangedListener = listener;
	}
	
	public void setChecked(int id) {
		mCheckedId = id;
		setChecked();
	}
	
	public void setChecked(View v) {
		setChecked(v.getId());
	}
	
	void setChecked() {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof Button) {
				setChecked((Button) child, (child.getId() == mCheckedId));
			}
		}
	}
	
	void setChecked(Button v, boolean checked) {
		if (checked) {
			v.setTypeface(null, Typeface.BOLD);
			v.setTextColor(Color.RED);
		} else {
			v.setTypeface(null, Typeface.NORMAL);
			v.setTextColor(Color.BLACK);
		}
	}
	
	float getCommonTextSize() {
		float size = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof Button) {
				Button btn = (Button) child;
				if (size == 0 || btn.getTextSize() < size) {
					size = btn.getTextSize();
				}
			}
		}
		return size;
	}
 
}
