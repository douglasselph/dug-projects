package com.dugsolutions.spaceshipwarrior;

import com.badlogic.gdx.Gdx;

public class Adjust
{
	public static Adjust getInstance()
	{
        return sAdjust;
	}

    public static void Init()
    {
        new Adjust();
    }

	static Adjust	sAdjust;

	float			max, may;
	float			dax, day;
	float			lax, lay;

	public Adjust()
	{
		sAdjust = this;
	}

	public void inc(float x, float y)
	{
		max += x;
		may += y;
		lax += x;
		lay += y;
		dax = 0;
		day = 0;
	}

	public void next(float delta)
	{
        if (lax != 0)
        {
            dax = max * delta;
            lax -= dax;

            if ((max > 0 && lax < 0) || (max < 0 && lax > 0))
            {
                lax = 0;
            }
        }
        else
        {
            dax = 0;
        }
		if (lay != 0)
		{
			day = may * delta;
			lay -= day;

            if ((may > 0 && lay < 0) || (may < 0 && lay > 0))
            {
                lay = 0;
            }
		}
        else
        {
            day = 0;
        }
    }

	public float getDeltaX()
	{
		return dax;
	}

	public float getDeltaY()
	{
		return day;
	}
}
