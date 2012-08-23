package com.tipsolutions.jacket.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.tipsolutions.jacket.R;
import com.tipsolutions.jacket.math.Color4f;

public class ColorControls extends FrameLayout
{
	class BtnGroup implements Runnable
	{
		BtnWrapper[]	mBtns;
		int				mSelected;

		BtnGroup(int size)
		{
			mBtns = new BtnWrapper[size];
			mSelected = 0;
		}

		@Override
		public void run()
		{
			for (BtnWrapper btn : mBtns)
			{
				if (btn.isSelected())
				{
					btn.setSelected(false);
					break;
				}
			}
			mSelected = -1;
		}

		void set(int pos, int btnResId, int normalResId, int selectedResId)
		{
			mBtns[pos] = new BtnWrapper((ImageButton) findViewById(btnResId), normalResId, selectedResId, this);
		}

		public void setSelected(int which)
		{
			run();
			mBtns[which].setSelected(true);
		}

		public int getSelected()
		{
			for (int i = 0; i < mBtns.length; i++)
			{
				if (mBtns[i].isSelected())
				{
					return i;
				}
			}
			return -1;
		}

		public BtnWrapper get(int code)
		{
			return mBtns[code];
		}
	};

	class BtnWrapper implements OnClickListener
	{
		final ImageButton	mBtn;
		boolean				mIsSelected;
		Runnable			mListener;
		final int			mNormalResId;
		final int			mSelectedResId;

		public BtnWrapper(ImageButton btn, int normalResId, int selectedResId, Runnable listener)
		{
			mNormalResId = normalResId;
			mSelectedResId = selectedResId;
			mBtn = btn;

			mBtn.setOnClickListener(this);
			mListener = listener;
		}

		public boolean isSelected()
		{
			return mIsSelected;
		}

		@Override
		public void onClick(View v)
		{
			mListener.run();
			setSelected(!mIsSelected);
		}

		public void setSelected(boolean flag)
		{
			mIsSelected = flag;
			if (mIsSelected)
			{
				mBtn.setImageResource(mSelectedResId);
			}
			else
			{
				mBtn.setImageResource(mNormalResId);
			}
		}

		public void setEnabled(boolean flag)
		{
			mBtn.setEnabled(flag);
		}
	}

	public enum ColorCode
	{
		All, Red, Green, Blue
	};

	public interface OnOperation
	{
		int getCurValue(int what, Part part);

		Color4f getCurColor(int what, Part part);

		int getHasColor(int what, Part part);

		int getMaxValue(int what, Part part);

		int getMinValue(int what, Part part);

		void setCurValue(int what, Part part, ColorCode code, int value);

		void setCurValue(int what, Part part, int value);
	};

	public enum Part
	{
		Ambient, Diffuse, Specular, Shininess
	};

	class WhatGroup implements OnClickListener
	{
		ArrayList<Integer>	mList	= new ArrayList<Integer>();
		final ImageButton	mBtn;
		int					mSelected;

		WhatGroup(ImageButton btn)
		{
			mBtn = btn;
			mBtn.setOnClickListener(this);
			mList.add(R.drawable.sun);
		}

		int getSelectCode()
		{
			return mSelected;
		}

		@Override
		public void onClick(View v)
		{
			if (++mSelected >= mList.size())
			{
				mSelected = 0;
			}
			mBtn.setImageResource(mList.get(mSelected));
		}

		public int add(int resId)
		{
			mList.add(resId);
			return mList.size() - 1;
		}

	};

	BtnGroup		mColors;
	BtnGroup		mParts;
	SeekBar			mSeekBar;
	WhatGroup		mWhat;
	OnOperation		mOpListener;
	LinearLayout	mColorLayout;

	public ColorControls(Context context)
	{
		super(context);
		setup();
		setControls();
	}

	public ColorControls(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setup();
		setControls();
	}

	public ColorControls(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setup();
		setControls();
	}

	public void setOpListener(OnOperation listener)
	{
		mOpListener = listener;
	}

	void setup()
	{
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout top = (RelativeLayout) inflater.inflate(R.layout.lightcontrol, null);
		addView(top, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mColors = new BtnGroup(4);
		mColors.set(0, R.id.color_all, R.drawable.empty, R.drawable.empty_select);
		mColors.set(1, R.id.color_red, R.drawable.empty, R.drawable.empty_select);
		mColors.set(2, R.id.color_green, R.drawable.empty, R.drawable.empty_select);
		mColors.set(3, R.id.color_blue, R.drawable.empty, R.drawable.empty_select);
		mColors.setSelected(0);
		mParts = new BtnGroup(4);
		mParts.set(0, R.id.part_ambient, R.drawable.ambient, R.drawable.ambient_select);
		mParts.set(1, R.id.part_diffuse, R.drawable.empty, R.drawable.empty_select);
		mParts.set(2, R.id.part_specular, R.drawable.spotlight, R.drawable.spotlight_select);
		mParts.set(3, R.id.part_shine, R.drawable.sparkle, R.drawable.sparkle_select);
		mParts.setSelected(0);

		mWhat = new WhatGroup((ImageButton) findViewById(R.id.what_selector));
		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mColorLayout = (LinearLayout) findViewById(R.id.colors);
	}

	void setControls()
	{
		if (mOpListener == null)
		{
			return;
		}
		int partSelected = mParts.getSelected();
		if (partSelected == 3)
		{
			mColors.get(1).setEnabled(false);
			mColors.get(2).setEnabled(false);
			mColors.get(3).setEnabled(false);
		}
	}

	public int getWhat()
	{
		return mWhat.getSelectCode();
	}

	public int addWhat(int resId)
	{
		return mWhat.add(resId);
	}
}
