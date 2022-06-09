package com.dugsolutions.jacket.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import com.dugsolutions.jacket.misc.Err;
import com.dugsolutions.jacket.misc.Msg;

public class Texture
{
	public interface Initializer
	{
		InputStream open();
	}

	static int			mDefaultBlendParam	= GL10.GL_MODULATE;
	static int			mDefaultBlendSource	= GL10.GL_ONE;
	static int			mDefaultBlendDest	= GL10.GL_ONE_MINUS_SRC_ALPHA;

	final Initializer	mInitializer;
	int					mTextureID			= 0;
	int					mBlendParam			= mDefaultBlendParam;
	int					mBlendSource		= mDefaultBlendSource;
	int					mBlendDest			= mDefaultBlendDest;

	public Texture(Initializer init)
	{
		mInitializer = init;
	}

	public Texture(final Texture from)
	{
		mInitializer = from.mInitializer;
		mTextureID = from.mTextureID;
		mBlendParam = from.mBlendParam;
		mBlendSource = from.mBlendSource;
		mBlendDest = from.mBlendDest;
	}

	public int getTextureID()
	{
		return mTextureID;
	}

	void init(GL10 gl) throws IOException
	{
		if (!initialized())
		{
			InputStream is = mInitializer.open();
			if (is != null)
			{
				init(is, gl);
				is.close();
			}
		}
	}

	void init(InputStream is, GL10 gl)
	{
		int[] textures = new int[1];
		gl.glGenTextures(1, textures, 0);
		mTextureID = textures[0];

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

		Bitmap bitmap = null;
		try
		{
			bitmap = BitmapFactory.decodeStream(is);
		}
		catch (Exception ex)
		{
			Msg.err(ex.getMessage());
		}
		if (bitmap != null)
		{
			// REALLY REALLY bad idea to do this kind of work here.
			// It is VERY slow:
			// Bitmap flipped = ImageUtils.FlipRows(bitmap);

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			Err.printErrors(gl);

			bitmap.recycle();
		}
	}

	public boolean initialized()
	{
		return mTextureID != 0;
	}

	public void load(GL10 gl)
	{
		if (!initialized())
		{
			try
			{
				init(gl);
			}
			catch (Exception ex)
			{
				Msg.err(ex.getMessage());
			}
		}
	}

	public void onDraw(GL10 gl, FloatBuffer fbuf)
	{
		load(gl);

		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(mBlendSource, mBlendDest);

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, mBlendParam);

		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fbuf);
	}

	public void reload(GL10 gl)
	{
		mTextureID = 0;
		load(gl);
	}

	// Expected to be one of GL10.GL_MODULATE, GL10.GL_DECAL,
	// GL10.GL_BLEND, or GL10.GL_REPLACE;
	public void setBlendEnv(int param)
	{
		mBlendParam = param;
	}

	// Expected to be GL10.GL_ONE,
	// GL10.GL_ONE_MINUS_SRC_ALPHA, etc.
	public void setBlendFunc(int src, int dest)
	{
		mBlendSource = src;
		mBlendDest = dest;
	}
}
