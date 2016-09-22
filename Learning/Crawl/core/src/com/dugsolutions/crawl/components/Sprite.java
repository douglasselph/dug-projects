package com.dugsolutions.crawl.components;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Sprite extends Component
{
	public Sprite(String name)
	{
		this.name = name;
	}
	public TextureRegion	region;
	public String			name;
	public float			r, g, b, a, scaleX, scaleY, rotation;
	public int				x, y, width, height;
}