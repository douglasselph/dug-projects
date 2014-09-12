package com.dugsolutions.jacket.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetManager;

import com.dugsolutions.jacket.image.Texture.Initializer;
import com.dugsolutions.jacket.misc.Msg;

public class TextureManager implements ITextureManager
{
	final Context			mContext;
	HashMap<Long, Texture>	mMap	= new HashMap<Long, Texture>();
	AssetManager			mAM;

	public TextureManager(Context ctx)
	{
		mContext = ctx;
		mAM = mContext.getResources().getAssets();
	}

	public Context getContext()
	{
		return mContext;
	}

	public Texture getTexture(final int resId)
	{
		Texture entry;
		long key = (1 << 16) + resId;
		if (mMap.containsKey(key))
		{
			entry = mMap.get(key);
		}
		else
		{
			entry = new Texture(new Initializer()
			{
				@Override
				public InputStream open()
				{
					return open_(resId);
				}
			});
			if (entry != null)
			{
				mMap.put(key, entry);
			}
		}
		return entry;
	}

	public Texture getTexture(final String filename)
	{
		Texture entry;
		long key = filename.hashCode();
		if (mMap.containsKey(key))
		{
			entry = mMap.get(key);
		}
		else
		{
			entry = new Texture(new Initializer()
			{
				@Override
				public InputStream open()
				{
					return open_(filename);
				}

			});
			if (entry != null)
			{
				mMap.put(key, entry);
			}
		}
		return entry;
	}

	public Collection<Texture> getTextures()
	{
		return mMap.values();
	}

	public void init(GL10 gl) throws IOException
	{
		for (Texture tex : getTextures())
		{
			tex.init(gl);
		}
	}

	public void load(GL10 gl)
	{
		for (Texture tex : getTextures())
		{
			tex.load(gl);
		}
	}

	InputStream open_(String filename)
	{
		InputStream is = null;
		try
		{
			is = mAM.open(filename);
		}
		catch (Exception ex)
		{
			Msg.err("File: \"", filename, "\", got exception: ", ex.getMessage());
		}
		return is;
	}

	InputStream open_(int resId)
	{
		InputStream is = null;
		try
		{
			is = mContext.getResources().openRawResource(resId);
		}
		catch (Exception ex)
		{
			Msg.err(ex.getMessage());
		}
		return is;
	}

	public void reload(GL10 gl)
	{
		for (Texture tex : getTextures())
		{
			tex.reload(gl);
		}
	}

	public void reset()
	{
		mMap = new HashMap<Long, Texture>();
	}

	public void setBlendParam(int param)
	{
		setDefaultBlendParam(param);
		for (Texture t : getTextures())
		{
			t.setBlendEnv(param);
		}
	}

	public void setDefaultBlendParam(int param)
	{
		Texture.mDefaultBlendParam = param;
	}
}
