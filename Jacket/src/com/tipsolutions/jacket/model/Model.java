package com.tipsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Bounds3D;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.ComputeBounds;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class Model {

	protected Bounds3D mBounds;
	protected Color4f mColor;
	protected FloatBuffer mColorBuf;
	protected Color4f mColorOutline = null;
	protected int mCullFace = GL10.GL_BACK;
	protected ShortBuffer mIndexBuf;
	protected int mIndexSlice;
	protected int mIndexMode = GL10.GL_TRIANGLES;
	protected Matrix4f mMatrix;
	protected Matrix4f mMatrixMod;
	protected FloatBuffer mNormalBuf;
	protected TextureManager.Texture mTexture;
	protected FloatBuffer mTextureBuf;
	protected FloatBuffer mVertexBuf;

	protected void computeBounds(ComputeBounds computeBounds) {
		if (mVertexBuf != null) {
			mVertexBuf.rewind();
			while (mVertexBuf.position() < mVertexBuf.limit()) {
				computeBounds.apply(mVertexBuf.get(), mVertexBuf.get(), mVertexBuf.get());
			}
		}
	}

	public Bounds3D getBounds() {
		if (mBounds == null) {
			ComputeBounds computeBounds = new ComputeBounds();
			computeBounds(computeBounds);
			mBounds = computeBounds.getBounds();
		}
		return mBounds;
	}

	public Color4f getColor() {
		return mColor;
	}

	public FloatBuffer getColorBuf() {
		return mColorBuf;
	}

	public int getCullFace() { 
		return mCullFace; 
	}

	protected int getFrontFace() { 
		return GL10.GL_CCW; 
	}
	
	public ShortBuffer getIndexBuf() { 
		return mIndexBuf;
	}

	public Vector3f getLocationMod() { 
		return getMatrixMod().getLocation(); 
	}

	// Returns the currently active matrix that should be applied for drawing.
	// Warning: this can return NULL.
	protected Matrix4f getMatrix() {
		if (mMatrixMod != null) {
			return mMatrixMod;
		}
		return mMatrix;
	}	

	// Get the modification matrix that lives on top of the object matrix.
	// Will never return null.
	public Matrix4f getMatrixMod() {
		if (mMatrixMod == null) {
			mMatrixMod = new Matrix4f(mMatrix);
		}
		return mMatrixMod;
	}

	public Vector3f getMidPoint() {
		Bounds3D bounds = getBounds();
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(), bounds.getMidZ());
		Matrix4f matrix = getMatrix();
		if (matrix != null) {
			matrix.multMV(midPoint);
		}
		return midPoint;
	}

	public FloatBuffer getNormalBuf() { 
		return mNormalBuf;
	}

	public Quaternion getQuaternionMod() { 
		return getMatrixMod().getQuaternion();
	}

	public FloatBuffer getTextureBuf() { 
		return mTextureBuf;
	}

	public FloatBuffer getVertexBuf() {
		return mVertexBuf;
	}

	public boolean hasColorArray() {
		return (mColorBuf != null);
	}

	public boolean hasNormalArray() {
		return (mNormalBuf != null);
	}

	public boolean hasTextureArray() {
		return (mTextureBuf != null);
	}

	public boolean hasVertexArray() {
		return (mVertexBuf != null);
	}

	public void onDraw(MatrixTrackingGL gl) {

		boolean didPush = false;

		if (mColorOutline != null) {
			gl.glColor4f(mColorOutline.getRed(), mColorOutline.getGreen(), mColorOutline.getBlue(), mColorOutline.getAlpha());
		} else if (!hasColorArray()) {
			Color4f color = getColor();
			if (color != null) {
				gl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			}
		}
		if (getFrontFace() != gl.getFrontFace()) {
			gl.glFrontFace(getFrontFace());
		}
		if (hasTextureArray()) {
			gl.setCullFace(0);
		} else {
			gl.setCullFace(getCullFace());
		}
		gl.glDisable(GL10.GL_BLEND);

		Matrix4f matrix = getMatrix();
		if (matrix != null) {
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			didPush = true;

			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(matrix);
			gl.glLoadMatrix(useMatrix);
		}
		if (mVertexBuf != null) {
			mVertexBuf.rewind();
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuf);
		} else {
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
		if (mColorOutline != null) {
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		} else {
			if (mNormalBuf != null) {
				mNormalBuf.rewind();
				gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
				gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuf);
			} else {
				gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
			}
			if (mColorBuf != null) {
				mColorBuf.rewind();
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
				gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuf);

				// Not doing it this way anymore:
				// gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
			} else {
				gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			}
			if ((mTexture != null) && (mTextureBuf != null)) {
				mTextureBuf.rewind();
				mTexture.onDraw(gl, mTextureBuf);
			} else {
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			}
		}
		if (mIndexBuf != null) {
			mIndexBuf.rewind();
			if (mIndexSlice > 0) {
				int count;
				while (mIndexBuf.position() < mIndexBuf.remaining()) {
					if (mIndexSlice < mIndexBuf.remaining()) {
						count = mIndexSlice;
					} else {
						count = mIndexBuf.remaining();
					}
					gl.glDrawElements(mIndexMode, count, GL10.GL_UNSIGNED_SHORT, mIndexBuf);
				}
			} else {
				gl.glDrawElements(mIndexMode, mIndexBuf.remaining(), GL10.GL_UNSIGNED_SHORT, mIndexBuf);
			}
		}
		onDrawing(gl);

		if (didPush) {
			gl.glPopMatrix();
		}
	}

	protected void onDrawing(MatrixTrackingGL gl) {
	}
	
	public void setColor(Color4f color) { 
		mColor = color; 
	}

	public void setIndexBuf(ShortBuffer buf) {
		mIndexBuf = buf;
		mIndexMode = GL10.GL_TRIANGLES;
		mIndexSlice = 0;
	}

	public void setIndexBuf(ShortBuffer buf, int mode) {
		mIndexBuf = buf;
		mIndexMode = mode;
		mIndexSlice = 0;
	}
	
	public void setIndexTriStrip(ShortBuffer buf, int slice) {
		mIndexBuf = buf;
		mIndexMode = GL10.GL_TRIANGLE_STRIP;
		mIndexSlice = slice;
	}
	
	public void setLocation(Vector3f x) { 
		getMatrixMod().setLocation(x);
	}

	public void setMatrixMod(Matrix4f mod) {
		mMatrixMod = mod;
	}

	public void setNormalBuf(FloatBuffer buf) {
		mNormalBuf = buf;
	}

	public void setTexture(Texture texture) {
		mTexture = texture;
	}
	
	public void setTextureBuf(FloatBuffer buf) {
		mTextureBuf = buf;
	}

	public void setVertexBuf(FloatBuffer buf) {
		mVertexBuf = buf;
	}
}
