package com.dugsolutions.spaceshipwarrior.components;

import com.artemis.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/**
 * Created by dug on 8/25/16.
 */
public class Sprite extends Component
{
	public enum Layer
	{
		DEFAULT, BACKGROUND, ACTORS_1, ACTORS_2, ACTORS_3, PARTICLES;

		public int getLayerId()
		{
			return ordinal();
		}
	}

	public Sprite(String name, Layer layer)
	{
		this.name = name;
		this.layer = layer;
	}

	public Sprite(String name)
	{
		this(name, Layer.DEFAULT);
	}

	public Sprite()
	{
		this("default", Layer.DEFAULT);
	}

	public String	name;
	public float	r		= 1;
	public float	g		= 1;
	public float	b		= 1;
	public float	a		= 1;
	public float	scaleX	= 1;
	public float	scaleY	= 1;
	public float	rotation;
	public Layer	layer	= Layer.DEFAULT;

}
