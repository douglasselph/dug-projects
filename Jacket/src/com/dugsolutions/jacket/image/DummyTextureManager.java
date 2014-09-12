package com.dugsolutions.jacket.image;

import java.io.InputStream;

import com.dugsolutions.jacket.image.Texture.Initializer;

public class DummyTextureManager implements ITextureManager
{
	@Override
	public Texture getTexture(final String filename)
	{
		return new Texture(new Initializer()
		{
			@Override
			public InputStream open()
			{
				System.out.println("Dummy texture manager can't open files");
				return null;
			}
		});
	}

}
