package com.tipsolutions.jacket.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class ToggleGroup extends LinearLayout {
	
	protected class CheckedStateTracker implements CompoundButton.OnCheckedChangeListener {
	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	        // prevents from infinite recursion
	        if (mProtectFromCheckedChange) {
	            return;
	        }
	        mProtectFromCheckedChange = true;
	        if (mCheckedId != -1) {
	            setCheckedStateForView(mCheckedId, false);
	        }
	        mProtectFromCheckedChange = false;
	        int id = buttonView.getId();
	        setCheckedId(id);
	    }
	}
	/**
	 * <p>This set of layout parameters defaults the width and the height of
	 * the children to {@link #WRAP_CONTENT} when they are not specified in the
	 * XML file. Otherwise, this class ussed the value read from the XML file.</p>
	 *
	 * <p>See
	 * {@link android.R.styleable#LinearLayout_Layout LinearLayout Attributes}
	 * for a list of all child view attributes that this class supports.</p>
	 *
	 */
	public static class LayoutParams extends LinearLayout.LayoutParams {
	    /**
	     * {@inheritDoc}
	     */
	    public LayoutParams(Context c, AttributeSet attrs) {
	        super(c, attrs);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public LayoutParams(int w, int h) {
	        super(w, h);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public LayoutParams(int w, int h, float initWeight) {
	        super(w, h, initWeight);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public LayoutParams(MarginLayoutParams source) {
	        super(source);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public LayoutParams(ViewGroup.LayoutParams p) {
	        super(p);
	    }

	    /**
	     * <p>Fixes the child's width to
	     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} and the child's
	     * height to  {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
	     * when not specified in the XML file.</p>
	     *
	     * @param a the styled attributes set
	     * @param widthAttr the width attribute to fetch
	     * @param heightAttr the height attribute to fetch
	     */
	    @Override
	    protected void setBaseAttributes(TypedArray a,
	            int widthAttr, int heightAttr) {

	        if (a.hasValue(widthAttr)) {
	            width = a.getLayoutDimension(widthAttr, "layout_width");
	        } else {
	            width = WRAP_CONTENT;
	        }

	        if (a.hasValue(heightAttr)) {
	            height = a.getLayoutDimension(heightAttr, "layout_height");
	        } else {
	            height = WRAP_CONTENT;
	        }
	    }
	}
	/**
	 * <p>Interface definition for a callback to be invoked when the checked
	 * radio button changed in this group.</p>
	 */
	public interface OnCheckedChangeListener {
	    /**
	     * <p>Called when the checked radio button has changed. When the
	     * selection is cleared, checkedId is -1.</p>
	     *
	     * @param group the group in which the checked radio button has changed
	     * @param checkedId the unique identifier of the newly checked radio button
	     */
	    public void onCheckedChanged(ToggleGroup group, int checkedId);
	}
	/**
	 * <p>A pass-through listener acts upon the events and dispatches them
	 * to another listener. This allows the table layout to set its own internal
	 * hierarchy change listener without preventing the user to setup his.</p>
	 */
	protected class PassThroughHierarchyChangeListener implements
	        ViewGroup.OnHierarchyChangeListener {
	    ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener;
	    /**
	     * {@inheritDoc}
	     */
	    public void onChildViewAdded(View parent, View child) {
	        if (parent == ToggleGroup.this) {
	        	if (child instanceof ToggleButton) {
	        		int id = child.getId();
	        		// generates an id if it's missing
	        		if (id == View.NO_ID) {
	        			id = child.hashCode();
	        			child.setId(id);
	        		}
	        		((ToggleButton) child).setOnCheckedChangeListener(mChildOnCheckedChangeListener);
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
	         if (parent == ToggleGroup.this && child instanceof ToggleButton) {
	             ((ToggleButton) child).setOnCheckedChangeListener(null);
	           // ((RadioButton) child).setOnCheckedChangeWidgetListener(null);
	        }

	        if (mOnHierarchyChangeListener != null) {
	            mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
	        }
	    }
	}
	// holds the checked id; the selection is empty by default
	protected int mCheckedId = -1;

	// tracks children radio buttons checked state
	protected CompoundButton.OnCheckedChangeListener mChildOnCheckedChangeListener;

	// when true, mOnCheckedChangeListener discards events
	protected boolean mProtectFromCheckedChange = false;

	protected OnCheckedChangeListener mOnCheckedChangeListener;
	protected PassThroughHierarchyChangeListener mPassThroughListener;

	/**
	 * {@inheritDoc}
	 */
	public ToggleGroup(Context context) {
	    super(context);
	    setOrientation(HORIZONTAL);
	    init();
	}

	/**
	 * {@inheritDoc}
	 */
	public ToggleGroup(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init();
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
	    if (child instanceof ToggleButton) {
	        final ToggleButton button = (ToggleButton) child;
	        if (button.isChecked()) {
	            mProtectFromCheckedChange = true;
	            if (mCheckedId != -1) {
	                setCheckedStateForView(mCheckedId, false);
	            }
	            mProtectFromCheckedChange = false;
	            setCheckedId(button.getId());
	        }
	    }
	    super.addView(child, index, params);
	}

	/**
	 * <p>Sets the selection to the radio button whose identifier is passed in
	 * parameter. Using -1 as the selection identifier clears the selection;
	 * such an operation is equivalent to invoking {@link #clearCheck()}.</p>
	 *
	 * @param id the unique id of the radio button to select in this group
	 *
	 * @see #getCheckedRadioButtonId()
	 * @see #clearCheck()
	 */
	public void check(int id) {
	    // don't even bother
	    if (id != -1 && (id == mCheckedId)) {
	        return;
	    }

	    if (mCheckedId != -1) {
	        setCheckedStateForView(mCheckedId, false);
	    }

	    if (id != -1) {
	        setCheckedStateForView(id, true);
	    }

	    setCheckedId(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
	    return p instanceof ToggleGroup.LayoutParams;
	}

	/**
	 * <p>Clears the selection. When the selection is cleared, no radio button
	 * in this group is selected and {@link #getCheckedRadioButtonId()} returns
	 * null.</p>
	 *
	 * @see #check(int)
	 * @see #getCheckedRadioButtonId()
	 */
	public void clearCheck() {
	    check(-1);
	}

	@Override
	protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
	    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
	    return new ToggleGroup.LayoutParams(getContext(), attrs);
	}

	/**
	 * <p>Returns the identifier of the selected radio button in this group.
	 * Upon empty selection, the returned value is -1.</p>
	 *
	 * @return the unique id of the selected radio button in this group
	 *
	 * @see #check(int)
	 * @see #clearCheck()
	 */
	public int getCheckedRadioButtonId() {
	    return mCheckedId;
	}

	private void init() {
	    mChildOnCheckedChangeListener = new CheckedStateTracker();
	    mPassThroughListener = new PassThroughHierarchyChangeListener();
	    super.setOnHierarchyChangeListener(mPassThroughListener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onFinishInflate() {
	    super.onFinishInflate();

	    // checks the appropriate radio button as requested in the XML file
	    if (mCheckedId != -1) {
	        mProtectFromCheckedChange = true;
	        setCheckedStateForView(mCheckedId, true);
	        mProtectFromCheckedChange = false;
	        setCheckedId(mCheckedId);
	    }
	}

	private void setCheckedId(int id) {
	    mCheckedId = id;
	    if (mOnCheckedChangeListener != null) {

	        //mOnCheckedChangeListener.

	        mOnCheckedChangeListener.onCheckedChanged(this, mCheckedId);
	    }
	}

	private void setCheckedStateForView(int viewId, boolean checked) {
	    View checkedView = findViewById(viewId);
	    if (checkedView != null) {
	    	if (checkedView instanceof ToggleButton) {
    	        ((ToggleButton) checkedView).setChecked(checked);
	    	}
	    }
	}

	/**
	 * <p>Register a callback to be invoked when the checked radio button
	 * changes in this group.</p>
	 *
	 * @param listener the callback to call on checked state change
	 */
	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
	    mOnCheckedChangeListener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
	    // the user listener is delegated to our pass-through listener
	    mPassThroughListener.mOnHierarchyChangeListener = listener;
	}
}
