package com.dugsolutions.crawl.core;

/**
 * Created by dug on 8/28/16.
 */
public class SimpleBounds
{
	int	x;
	int	y;
	int	width;
	int	height;

	public SimpleBounds(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public int getMinX()
	{
		return x;
	}

	public int getMinY()
	{
		return y;
	}

	public int getMaxX()
	{
		return x + width - 1;
	}

	public int getMaxY()
	{
		return y + height - 1;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public int getMidX()
	{
		return x + width / 2;
	}

	public int getMidY()
	{
		return y + height / 2;
	}
}
