package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;

import android.graphics.Color;

public class Color4f
{
	/**
	 * the color black (0, 0, 0, 1).
	 */
	public static final Color4f	BLACK			= new Color4f(0f, 0f, 0f, 1f);
	/**
	 * the color black with a zero alpha value (0, 0, 0, 0).
	 */
	public static final Color4f	BLACK_NO_ALPHA	= new Color4f(0f, 0f, 0f, 0f);
	/**
	 * the color white (1, 1, 1, 1).
	 */
	public static final Color4f	WHITE			= new Color4f(1f, 1f, 1f, 1f);
	/**
	 * the color gray (.2f, .2f, .2f, 1).
	 */
	public static final Color4f	DARK_GRAY		= new Color4f(0.2f, 0.2f, 0.2f, 1.0f);
	/**
	 * the color gray (.5f, .5f, .5f, 1).
	 */
	public static final Color4f	GRAY			= new Color4f(0.5f, 0.5f, 0.5f, 1.0f);
	/**
	 * the color gray (.8f, .8f, .8f, 1).
	 */
	public static final Color4f	LIGHT_GRAY		= new Color4f(0.8f, 0.8f, 0.8f, 1.0f);
	/**
	 * the color red (1, 0, 0, 1).
	 */
	public static final Color4f	RED				= new Color4f(1f, 0f, 0f, 1f);
	/**
	 * the color green (0, 1, 0, 1).
	 */
	public static final Color4f	GREEN			= new Color4f(0f, 1f, 0f, 1f);
	/**
	 * the color blue (0, 0, 1, 1).
	 */
	public static final Color4f	BLUE			= new Color4f(0f, 0f, 1f, 1f);
	/**
	 * the color yellow (1, 1, 0, 1).
	 */
	public static final Color4f	YELLOW			= new Color4f(1f, 1f, 0f, 1f);
	/**
	 * the color magenta (1, 0, 1, 1).
	 */
	public static final Color4f	MAGENTA			= new Color4f(1f, 0f, 1f, 1f);
	/**
	 * the color cyan (0, 1, 1, 1).
	 */
	public static final Color4f	CYAN			= new Color4f(0f, 1f, 1f, 1f);
	/**
	 * the color orange (251/255f, 130/255f, 0, 1).
	 */
	public static final Color4f	ORANGE			= new Color4f(251f / 255f, 130f / 255f, 0f, 1f);
	/**
	 * the color brown (65/255f, 40/255f, 25/255f, 1).
	 */
	public static final Color4f	BROWN			= new Color4f(65f / 255f, 40f / 255f, 25f / 255f, 1f);
	/**
	 * the color pink (1, 0.68f, 0.68f, 1).
	 */
	public static final Color4f	PINK			= new Color4f(1f, 0.68f, 0.68f, 1f);

	static final int			IRED			= 0;
	static final int			IGREEN			= 1;
	static final int			IBLUE			= 2;
	static final int			IALPHA			= 3;

	static protected int value255(float c)
	{
		return (int) (c * 255);
	}

	protected float[]	mColor	= new float[4];

	public Color4f(final Color4f c)
	{
		for (int i = 0; i < mColor.length; i++)
		{
			mColor[i] = c.mColor[i];
		}
	}

	public Color4f(float r, float g, float b)
	{
		mColor[IRED] = r;
		mColor[IGREEN] = g;
		mColor[IBLUE] = b;
		mColor[IALPHA] = 1;
	}

	public Color4f(float r, float g, float b, float a)
	{
		mColor[IRED] = r;
		mColor[IGREEN] = g;
		mColor[IBLUE] = b;
		mColor[IALPHA] = a;
	}

	public Color4f(int color)
	{
		mColor[IRED] = ((float) Color.red(color)) / (float) 0xFF;
		mColor[IGREEN] = ((float) Color.green(color)) / (float) 0xFF;
		mColor[IBLUE] = ((float) Color.blue(color)) / (float) 0xFF;
		mColor[IALPHA] = ((float) Color.alpha(color)) / (float) 0xFF;
	}

	public boolean equals(final Color4f o)
	{
		for (int i = 0; i < mColor.length; i++)
		{
			if (mColor[i] != o.mColor[i])
			{
				return false;
			}
		}
		return true;
	}

	public float getAlpha()
	{
		return mColor[IALPHA];
	}

	public float getBlue()
	{
		return mColor[IBLUE];
	}

	public int getColor()
	{
		return Color.argb(value255(mColor[IALPHA]), value255(mColor[IRED]), value255(mColor[IGREEN]),
				value255(mColor[IBLUE]));
	}

	public float getGreen()
	{
		return mColor[IGREEN];
	}

	public float getRed()
	{
		return mColor[IRED];
	}

	public Color4f put(FloatBuffer buf)
	{
		buf.put(mColor[IRED]).put(mColor[IGREEN]).put(mColor[IBLUE]).put(mColor[IALPHA]);
		return this;
	}

	public void set(final Color4f cp)
	{
		for (int i = 0; i < mColor.length; i++)
		{
			mColor[i] = cp.mColor[i];
		}
	}

	public void set(float r, float g, float b, float a)
	{
		mColor[IRED] = r;
		mColor[IGREEN] = g;
		mColor[IBLUE] = b;
		mColor[IALPHA] = a;
	}

	public void setAlpha(float a)
	{
		mColor[IALPHA] = a;
	}

	public void setBlue(float b)
	{
		mColor[IBLUE] = b;
	}

	public void setGreen(float g)
	{
		mColor[IGREEN] = g;
	}

	public void setRed(float r)
	{
		mColor[IRED] = r;
	}

	public float[] toArray()
	{
		return mColor;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[");
		sbuf.append(getRed());
		sbuf.append(",");
		sbuf.append(getGreen());
		sbuf.append(",");
		sbuf.append(getBlue());
		sbuf.append(",");
		sbuf.append(getAlpha());
		sbuf.append("]");
		return sbuf.toString();
	}

}
