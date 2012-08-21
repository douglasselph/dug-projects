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
	protected Color4f					mColorDiffuse;
	protected Color4f					mColorAmbient;
	protected Color4f					mColorSpecular;
	protected Color4f					mColorEmission;
	protected Float						mColorShininess;

	protected void computeBounds(ComputeBounds computeBounds)
	{
		if (mVertexBuf != null)
		{
			mVertexBuf.rewind();
			while (mVertexBuf.position() < mVertexBuf.limit())
			{
				computeBounds.apply(mVertexBuf.get(), mVertexBuf.get(), mVertexBuf.get());
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
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(), bounds.getMidZ());
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

	public void onDraw(GL10 gl)
	{
		boolean didPush = false;

		onDrawPre(gl);

		Matrix4f matrix = getMatrix();
		if (matrix != null)
		{
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			didPush = true;
		}
		boolean hasMaterials = false;

		if (mColorAmbient != null)
		{
			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mColorAmbient.toArray(), 0);
			hasMaterials = true;
		}
		if (mColorDiffuse != null)
		{
			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mColorDiffuse.toArray(), 0);
			hasMaterials = true;
		}
		if (mColorEmission != null)
		{
			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, mColorEmission.toArray(), 0);
			hasMaterials = true;
		}
		if (mColorSpecular != null)
		{
			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mColorSpecular.toArray(), 0);
			hasMaterials = true;
		}
		if (mColorShininess != null)
		{
			gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mColorShininess);
			hasMaterials = true;
		}
		if (!hasMaterials && !hasColorArray() && mColor != null)
		{
			gl.glColor4f(mColor.getRed(), mColor.getGreen(), mColor.getBlue(), mColor.getAlpha());
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
		if (!hasMaterials && mColorBuf != null)
		{
			mColorBuf.rewind();
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuf.getBuf());
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
					gl.glDrawElements(mIndexMode, count, GL10.GL_UNSIGNED_SHORT, sbuf);
					position += count;
				}
			}
			else
			{
				gl.glDrawElements(mIndexMode, mIndexBuf.remaining(), GL10.GL_UNSIGNED_SHORT, mIndexBuf.getBuf());
			}
		}
		onDrawPost(gl);

		if (didPush)
		{
			gl.glPopMatrix();
		}
	}

	protected void onDrawPost(GL10 gl)
	{
	}

	protected void onDrawPre(GL10 gl)
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

	/**
	 * Ambient light is light that comes from all directions. Systems like OpenGL only directly simulate light coming
	 * from some light source. The don't simulate the natural occurrence of light bouncing off of other sources or being
	 * diffused by the atmosphere. Consequently, any surface that does not have a light shining on it directly is not
	 * lit at all by that light. A hack to deal with this problem is ambient light. If you set an ambient color the same
	 * as the diffuse color and use a small amount of ambient lighting, you'll be able to see all surfaces no matter
	 * where the light is. Because AMBIENT and DIFFUSE are often set at the same time, there is an
	 * GL_AMBIENT_AND_DIFFUSE option for glMaterial.
	 * 
	 * @param color
	 */
	public void setColorAmbient(Color4f color)
	{
		mColorAmbient = color;
	}

	/**
	 * Surfaces can be considered to have two lighting characteristics: diffuse reflection and specular reflection.
	 * Diffuse reflection reflects light in all directions, regardless of where it came from. Specular reflection
	 * reflects more light in the mirror direction. A perfect mirror has no diffuse reflection and tons of specular
	 * reflection. Perfectly flat paint has diffuse reflection and no specular reflection. Most things are in-between.
	 * This is generally the property you'll use to set the color of a surface.
	 * 
	 * @param color
	 */
	public void setColorDiffuse(Color4f color)
	{
		mColorDiffuse = color;
	}

	/**
	 * The emissive property is how much a surface generates it's own light.
	 * 
	 * @param color
	 */
	public void setColorEmission(Color4f color)
	{
		mColorEmission = color;
	}

	/**
	 * This property sets the specular color. Note that the specular color for most surfaces is white, even if the
	 * surface is a different color.
	 * 
	 * @param color
	 */
	public void setColorSpecular(Color4f color)
	{
		mColorSpecular = color;
	}

	/**
	 * This determine how shiny a surface is. Values range from 0 to 128.
	 * 
	 * @param shininess
	 */
	public void setColorShininess(float shininess)
	{
		mColorShininess = shininess;
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

		sbuf.append("VERTEX=\n");
		sbuf.append(mVertexBuf.toString(3));
		sbuf.append("\n");

		if (mNormalBuf != null)
		{
			sbuf.append("NORMAL=\n");
			sbuf.append(mNormalBuf.toString(3));
			sbuf.append("\n");
		}
		if (mTextureBuf != null)
		{
			sbuf.append("TEX=\n");
			sbuf.append(mTextureBuf.toString(2));
			sbuf.append("\n");
		}
		if (mColorBuf != null)
		{
			sbuf.append("COLOR=\n");
			sbuf.append(mColorBuf.toString(4));
			sbuf.append("\n");
		}
		sbuf.append("INDEX=\n");
		sbuf.append(mIndexBuf.toString());
		sbuf.append("\n");
		return sbuf.toString();
	}

}
