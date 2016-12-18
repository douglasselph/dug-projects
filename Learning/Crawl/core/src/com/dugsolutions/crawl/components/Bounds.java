package com.dugsolutions.crawl.components;

import com.artemis.Component;

public class Bounds extends Component
{
	public Bounds(float radius)
	{
		this.radius = radius;
	}

	public Bounds()
	{
		this(0);
	}

	public float	radius;
}
