package com.tipsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Bounds3D;
import com.tipsolutions.jacket.math.BufUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufUtils.ShortBuf;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.ComputeBounds;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class Model
{
	static final String					TAG			= "Model";
	static final Boolean				LOG			= true;
	protected Bounds3D					mBounds;
	protected Color4f					mColor;
	protected FloatBuf					mColorBuf;
	protected ShortBuf					mIndexBuf;
	protected int						mIndexSlice;
	protected int						mIndexMode	= GL10.GL_TRIANGLES;
	protected Matrix4f					mMatrix;
	protected Matrix4f					mMatrixMod;
	protected FloatBuf					mNormalBuf;
	protected TextureManager.Texture	mTexture;
	protected FloatBuf					mTextureBuf;
	protected FloatBuf					mVertexBuf;

	protected void computeBounds(ComputeBounds computeBounds)
	{
		if (mVertexBuf != null)
		{
			mVertexBuf.rewind();
			while (mVertexBuf.position() < mVertexBuf.limit())
			{
				computeBounds.apply(mVertexBuf.get(), mVertexBuf.get(),
						mVertexBuf.get());
			}
		}
	}

	public Bounds3D getBounds()
	{
		if (mBounds == null)
		{
			ComputeBounds computeBounds = new ComputeBounds();
			computeBounds(computeBounds);
			mBounds = computeBounds.getBounds();
		}
		return mBounds;
	}

	public Color4f getColor()
	{
		return mColor;
	}

	public FloatBuffer getColorBuf()
	{
		return mColorBuf.getBuf();
	}

	public ShortBuffer getIndexBuf()
	{
		return mIndexBuf.getBuf();
	}

	public Vector3f getLocationMod()
	{
		return getMatrixMod().getLocation();
	}

	// Returns the currently active matrix that should be applied for drawing.
	// Warning: this can return NULL.
	protected Matrix4f getMatrix()
	{
		if (mMatrixMod != null)
		{
			return mMatrixMod;
		}
		return mMatrix;
	}

	// Get the modification matrix that lives on top of the object matrix.
	// Will never return null.
	public Matrix4f getMatrixMod()
	{
		if (mMatrixMod == null)
		{
			mMatrixMod = new Matrix4f(mMatrix);
		}
		return mMatrixMod;
	}

	public Vector3f getMidPoint()
	{
		Bounds3D bounds = getBounds();
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(),
				bounds.getMidZ());
		Matrix4f matrix = getMatrix();
		if (matrix != null)
		{
			matrix.multMV(midPoint);
		}
		return midPoint;
	}

	public FloatBuffer getNormalBuf()
	{
		return mNormalBuf.getBuf();
	}

	public Quaternion getQuaternionMod()
	{
		return getMatrixMod().getQuaternion();
	}

	public Texture getTexture()
	{
		return mTexture;
	}

	public FloatBuffer getTextureBuf()
	{
		return mTextureBuf.getBuf();
	}

	public FloatBuffer getVertexBuf()
	{
		return mVertexBuf.getBuf();
	}

	public boolean hasColorArray()
	{
		return (mColorBuf != null);
	}

	public boolean hasNormalArray()
	{
		return (mNormalBuf != null);
	}

	public boolean hasTextureArray()
	{
		return (mTextureBuf != null);
	}

	public boolean hasVertexArray()
	{
		return (mVertexBuf != null);
	}

	public FloatBuffer initColorBuf(int size)
	{
		mColorBuf = new FloatBuf(size);
		return mColorBuf.getBuf();
	}

	public ShortBuffer initIndexBuf(int size)
	{
		return initIndexBuf(size, GL10.GL_TRIANGLES);
	}

	public ShortBuffer initIndexBuf(int size, int mode)
	{
		mIndexBuf = new ShortBuf(size);
		mIndexMode = mode;
		mIndexSlice = 0;
		return mIndexBuf.getBuf();
	}

	public ShortBuffer initIndexTriStrip(int size, int slice)
	{
		mIndexBuf = new ShortBuf(size);
		mIndexMode = GL10.GL_TRIANGLE_STRIP;
		mIndexSlice = slice;
		return mIndexBuf.getBuf();
	}

	public FloatBuffer initNormalBuf(int size)
	{
		mNormalBuf = new FloatBuf(size);
		return mNormalBuf.getBuf();
	}

	public FloatBuffer initTextureBuf(int size)
	{
		mTextureBuf = new FloatBuf(size);
		return mTextureBuf.getBuf();
	}

	public FloatBuffer initVertexBuf(int size)
	{
		mVertexBuf = new FloatBuf(size);
		return mVertexBuf.getBuf();
	}

	public void onDraw(MatrixTrackingGL gl)
	{
		boolean didPush = false;

		onDrawPre(gl);

		Matrix4f matrix = getMatrix();
		if (matrix != null)
		{
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			didPush = true;

			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(matrix);
			gl.glLoadMatrix(useMatrix);
		}
		if (!hasColorArray() && mColor != null)
		{
			gl.glColor4f(mColor.getRed(), mColor.getGreen(), mColor.getBlue(),
					mColor.getAlpha());
		}
		if (mVertexBuf != null)
		{
			mVertexBuf.rewind();
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuf.getBuf());
		}
		else
		{
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
		if (mNormalBuf != null)
		{
			mNormalBuf.rewind();
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuf.getBuf());
		}
		else
		{
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
		if (mColorBuf != null)
		{
			mColorBuf.rewind();
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuf.getBuf());

			// Not doing it this way anymore:
			// gl.glColorPointer(4, GL10.GL_FIXED, 0,
			// mColorBuf.asShortBuffer());
		}
		else
		{
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
		if ((mTexture != null) && (mTextureBuf != null))
		{
			mTextureBuf.rewind();
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexture.getTextureID());
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuf.getBuf());
		}
		else
		{
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}
		if (mIndexBuf != null)
		{
			mIndexBuf.rewind();
			if (mIndexSlice > 0)
			{
				ShortBuffer sbuf = mIndexBuf.getBuf();

				int count;
				int position = 0;
				int remaining = sbuf.remaining();

				while (position < remaining)
				{
					if (mIndexSlice < remaining)
					{
						count = mIndexSlice;
					}
					else
					{
						count = remaining;
					}
					sbuf.position(position);
					gl.glDrawElements(mIndexMode, count,
							GL10.GL_UNSIGNED_SHORT, sbuf);
					position += count;
				}
			}
			else
			{
				gl.glDrawElements(mIndexMode, mIndexBuf.remaining(),
						GL10.GL_UNSIGNED_SHORT, mIndexBuf.getBuf());
			}
		}
		onDrawPost(gl);

		if (didPush)
		{
			gl.glPopMatrix();
		}
	}

	protected void onDrawPost(MatrixTrackingGL gl)
	{
	}

	protected void onDrawPre(MatrixTrackingGL gl)
	{
		gl.glFrontFace(GL10.GL_CW);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_BACK);

		// gl.glEnable(GL10.GL_DEPTH_TEST);
		// gl.glDisable(GL10.GL_BLEND);
	}

	public void setColor(Color4f color)
	{
		mColor = color;
	}

	public void setLocation(Vector3f x)
	{
		getMatrixMod().setLocation(x);
	}

	public void setMatrixMod(Matrix4f mod)
	{
		mMatrixMod = mod;
	}

	public Model setTexture(Texture texture)
	{
		mTexture = texture;
		return this;
	}

	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();

		sbuf.append("VERTEX=[");
		sbuf.append(mVertexBuf.toString());
		sbuf.append("]\n");

		if (mNormalBuf != null)
		{
			sbuf.append("NORMAL=[");
			sbuf.append(mNormalBuf.toString());
			sbuf.append("]\n");
		}
		if (mTextureBuf != null)
		{
			sbuf.append("TEX=[");
			sbuf.append(mTextureBuf.toString());
			sbuf.append("]\n");
		}
		if (mColorBuf != null)
		{
			sbuf.append("COLOR=[");
			sbuf.append(mColorBuf.toString());
			sbuf.append("]\n");
		}
		sbuf.append("INDEX=[");
		sbuf.append(mIndexBuf.toString());
		sbuf.append("]\n");
		return sbuf.toString();
	}

}
