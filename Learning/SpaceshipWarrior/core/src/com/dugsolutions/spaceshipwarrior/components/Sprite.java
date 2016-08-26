package com.dugsolutions.spaceshipwarrior.components;

import com.artemis.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/**
 * Created by dug on 8/25/16.
 */
public class Sprite extends Component
{
	public Sprite(String path)
	{
		sprite = new Texture(Gdx.files.internal(path));
	}

	public Sprite()
	{
		this("fighter.png");
	}

	public enum Layer
	{
		DEFAULT, BACKGROUND, ACTORS_1, ACTORS_2, ACTORS_3, PARTICLES;

		public int getLayerId()
		{
			return ordinal();
		}
	}

	public Texture	sprite;
	public String	name;
	public float	scaleX	= 1;
	public float	scaleY	= 1;
	public float	rotation;
	public float	r		= 1;
	public float	g		= 1;
	public float	b		= 1;
	public float	a		= 1;
	public Layer	layer	= Layer.DEFAULT;

}
