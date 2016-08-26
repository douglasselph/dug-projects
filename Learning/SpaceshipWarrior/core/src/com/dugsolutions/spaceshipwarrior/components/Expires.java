package com.dugsolutions.spaceshipwarrior.components;

import com.artemis.Component;

public class Expires extends Component
{
	public float	delay;

	public Expires(float delay)
	{
		this.delay = delay;
	}

    public boolean isExpired()
    {
        return delay <= 0;
    }

	public Expires()
	{
		this(0);
	}
}
