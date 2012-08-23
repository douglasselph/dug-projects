package com.tipsolutions.jacket.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tipsolutions.jacket.R;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MaterialColors;

public class ColorControls extends FrameLayout
{
	class BtnGroup implements SelectListener
	{
		BtnWrapper[]	mBtns;
		int				mSelected;
		SelectListener	mSelectListener;

		BtnGroup(int size)
		{
			mBtns = new BtnWrapper[size];
			mSelected = 0;
		}

		public BtnWrapper get(int code)
		{
			return mBtns[code];
		}

		public int getSelected()
		{
			return mSelected;
		}

		public int size()
		{
			return mBtns.length;
		}

		@Override
		public void select(final int code)
		{
			for (BtnWrapper btn : mBtns)
			{
				if (btn.isSelected())
				{
					btn.setSelected(false);
					break;
				}
			}
			mSelected = code;

			if (mSelectListener != null)
			{
				mSelectListener.select(code);
			}
		}

		void set(int pos, int btnResId, int normalResId, int selectedResId)
		{
			mBtns[pos] = new BtnWrapper((ImageButton) findViewById(btnResId), normalResId, selectedResId, this, pos);
		}

		public void setSelected(int which)
		{
			select(which);
			mBtns[which].setSelected(true);
		}

		public void setSelectListener(SelectListener listener)
		{
			mSelectListener = listener;
		}
	};

	class BtnWrapper implements OnClickListener
	{
		final ImageButton	mBtn;
		boolean				mIsSelected;
		SelectListener		mListener;
		final int			mNormalResId;
		final int			mSelectedResId;
		final int			mArg;

		public BtnWrapper(ImageButton btn, int normalResId, int selectedResId, SelectListener listener, int arg)
		{
			mNormalResId = normalResId;
			mSelectedResId = selectedResId;
			mBtn = btn;
			mArg = arg;

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
			mListener.select(mArg);
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

		public void setColor(Color4f color)
		{
			if (color != null)
			{
				mBtn.setBackgroundColor(color.getColor());
			}
		}
	};

	public enum ColorCode
	{
		All, Red, Green, Blue
	}

	public interface OnOperation
	{
		MaterialColors getMatColor(int what);

		int getValue(int what);

		boolean hasParts(int what);

		void valueChanged(int what, int value);

		void valueChanged(int what, MaterialColors value);
	};

	enum Part
	{
		Unknown, Ambient, Diffuse, Specular, Shininess
	};

	interface SelectListener
	{
		void select(int code);
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

		public int add(int resId)
		{
			mList.add(resId);
			return mList.size() - 1;
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
			controlInit();
		}

	};

	static final String	TAG	= "ColorControls";

	BtnGroup			mColors;
	BtnGroup			mParts;
	View[]				mShow;
	SeekBar				mSeekBar;
	WhatGroup			mWhat;
	OnOperation			mOpListener;
	View				mColorLayout;
	View				mPartLayout;

	public ColorControls(Context context)
	{
		super(context);
		setup();
		controlInit();
	}

	public ColorControls(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setup();
		controlInit();
	}

	public ColorControls(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setup();
		controlInit();
	}

	public int addWhat(int resId)
	{
		return mWhat.add(resId);
	}

	public void controlChanged(final int value)
	{
		if (mOpListener == null)
		{
			return;
		}
		int whatSelected = mWhat.getSelectCode();

		if (mOpListener.hasParts(whatSelected))
		{
			Part part = getPart(mParts.getSelected());
			MaterialColors matColors = mOpListener.getMatColor(whatSelected);

			if (matColors != null)
			{
				if (part == Part.Shininess)
				{
					float nValue = value * 127f;
					matColors.setShininess(nValue);
				}
				else
				{
					Color4f color = getColor(matColors, part);
					int colorSelected = mColors.getSelected();

					setColorValue(colorSelected, color, value);
				}
				mOpListener.valueChanged(whatSelected, matColors);
			}
		}
		else
		{
			mOpListener.valueChanged(whatSelected, value);
		}
	}

	void controlInit()
	{
		if (mOpListener == null)
		{
			return;
		}
		int whatSelected = mWhat.getSelectCode();

		if (mOpListener.hasParts(whatSelected))
		{
			Part part = getPart(mParts.getSelected());

			mPartLayout.setVisibility(View.VISIBLE);

			MaterialColors matColors = mOpListener.getMatColor(whatSelected);

			if (matColors != null)
			{
				if (part == Part.Shininess)
				{
					mColorLayout.setVisibility(View.INVISIBLE);
					mSeekBar.setMax(127);

					Float shine = matColors.getShininess();
					if (shine != null)
					{
						int value = (int) FloatMath.floor(shine * 127);
						mSeekBar.setProgress(value);
						mSeekBar.setEnabled(true);
					}
					else
					{
						mSeekBar.setEnabled(false);
					}
				}
				else
				{
					mColorLayout.setVisibility(View.VISIBLE);
					mSeekBar.setMax(255);

					int colorSelected = mColors.getSelected();
					Color4f color = getColor(matColors, part);

					if (color != null)
					{
						int value = getColorValue(colorSelected, color);
						mSeekBar.setProgress(value);
						mSeekBar.setEnabled(true);
					}
					else
					{
						mSeekBar.setEnabled(false);
					}
				}
				showColor(mShow[0], mParts.get(0), matColors.getAmbient());
				showColor(mShow[1], mParts.get(1), matColors.getDiffuse());
				showColor(mShow[2], mParts.get(2), matColors.getSpecular());

				if (matColors.getShininess() == null)
				{
					mParts.get(3).mBtn.setVisibility(View.INVISIBLE);
				}
				else
				{
					mParts.get(3).mBtn.setVisibility(View.VISIBLE);
				}
			}
			else
			{
				for (int i = 0; i < mParts.size(); i++)
				{
					mParts.get(i).mBtn.setVisibility(View.INVISIBLE);
				}
			}
		}
		else
		{
			mColorLayout.setVisibility(View.INVISIBLE);
			mPartLayout.setVisibility(View.INVISIBLE);

			int value = mOpListener.getValue(whatSelected);
			mSeekBar.setProgress(value);
			mSeekBar.setEnabled(true);
		}
	}

	Color4f getColor(MaterialColors matColors, Part part)
	{
		if (part == Part.Ambient)
		{
			return matColors.getAmbient();
		}
		if (part == Part.Diffuse)
		{
			return matColors.getDiffuse();
		}
		if (part == Part.Specular)
		{
			return matColors.getSpecular();
		}
		return null;
	}

	int getColorValue(int colorSelect, Color4f color)
	{
		if (color == null)
		{
			Log.e(TAG, "NULL color [" + colorSelect + "]");
			return 0;
		}
		switch (colorSelect)
		{
			case 0:
			{
				float value = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
				return (int) (FloatMath.floor(value * 255));
			}
			case 1:
				return (int) (FloatMath.floor(color.getRed() * 255));
			case 2:
				return (int) (FloatMath.floor(color.getGreen() * 255));
			case 3:
				return (int) (FloatMath.floor(color.getBlue() * 255));
		}
		Log.e(TAG, "bad color select " + colorSelect);
		return 0;
	}

	public OnOperation getOpListener()
	{
		return mOpListener;
	}

	Part getPart(int code)
	{
		switch (code)
		{
			case 0:
				return Part.Ambient;
			case 1:
				return Part.Diffuse;
			case 2:
				return Part.Specular;
			case 3:
				return Part.Shininess;
		}
		return Part.Unknown;
	}

	public int getWhat()
	{
		return mWhat.getSelectCode();
	}

	void setColorValue(int colorSelect, Color4f color, int value)
	{
		float cValue = (float) value / (float) 255;

		switch (colorSelect)
		{
			case 0:
			{
				color.setRed(cValue);
				color.setGreen(cValue);
				color.setBlue(cValue);
				break;
			}
			case 1:
				color.setRed(cValue);
				break;
			case 2:
				color.setGreen(cValue);
				break;
			case 3:
				color.setBlue(cValue);
				break;
		}
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
		mParts.setSelectListener(new SelectListener()
		{
			@Override
			public void select(int code)
			{
				controlInit();
			}
		});

		mWhat = new WhatGroup((ImageButton) findViewById(R.id.what_selector));
		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mColorLayout = findViewById(R.id.colors);
		mPartLayout = findViewById(R.id.part);

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if (fromUser)
				{
					controlChanged(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
		});

		mShow = new View[3];
		mShow[0] = findViewById(R.id.show_ambient);
		mShow[1] = findViewById(R.id.show_diffuse);
		mShow[2] = findViewById(R.id.show_specular);
	}

	void showColor(View view, BtnWrapper wrap, Color4f color)
	{
		if (color != null)
		{
			view.setBackgroundColor(color.getColor());

			view.setVisibility(View.VISIBLE);
			wrap.mBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			view.setVisibility(View.INVISIBLE);
			wrap.mBtn.setVisibility(View.INVISIBLE);
		}
	}

	public void update()
	{
		controlInit();
	}
}
