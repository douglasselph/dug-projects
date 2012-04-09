package com.tipsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.BufferUtils.Bounds;
import com.tipsolutions.jacket.math.BufferUtils.ComputeBounds;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.ShortBuf;
import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class Model {

	protected FloatBuf mColorBuf = new FloatBuf();
	protected ShortBuf mIndexBuf = new ShortBuf();
	protected FloatBuf mNormalBuf = new FloatBuf();
	protected FloatBuf mVertexBuf = new FloatBuf();
	protected FloatBuf mTextureBuf = new FloatBuf();
	protected int mIndexMode = GL10.GL_TRIANGLES;
	protected Matrix4f mMatrix;
	protected Matrix4f mMatrixMod;
	protected Color4f mColor;
	protected Color4f mColorOutline = null;
	protected TextureManager.Texture mTexture;
	protected int mCullFace = GL10.GL_BACK;
	protected Bounds mBounds;

	public FloatBuffer allocColorBuf(int size) {
		mColorBuf = new FloatBuf();
		FloatBuffer buf = mColorBuf.alloc(size);
		buf.rewind();
		return buf;
	}

	public ShortBuffer allocIndexBuf(int size) {
		mIndexBuf = new ShortBuf();
		ShortBuffer buf = mIndexBuf.alloc(size);
		buf.rewind();
		return buf;
	}

	public FloatBuffer allocNormalBuf(int size) {
		mNormalBuf = new FloatBuf();
		FloatBuffer buf = mNormalBuf.alloc(size);
		buf.rewind();
		return buf;
	}

	public FloatBuffer allocTextureBuf(int size) {
		mTextureBuf = new FloatBuf();
		FloatBuffer buf = mTextureBuf.alloc(size);
		buf.rewind();
		return buf;
	}

	public FloatBuffer allocVertexBuf(int size) {
		mVertexBuf = new FloatBuf();
		FloatBuffer buf = mVertexBuf.alloc(size);
		buf.rewind();
		return buf;
	}

	protected void computeBounds(ComputeBounds computeBounds) {
		FloatBuffer buf = getVertexBuf();
		if (buf != null) {
			while (buf.position() < buf.limit()) {
				computeBounds.apply(buf.get(), buf.get(), buf.get());
			}
		}
	}

	public Bounds getBounds() {
		if (mBounds == null) {
			ComputeBounds computeBounds = new ComputeBounds();
			computeBounds(computeBounds);
			mBounds.set(computeBounds);
		}
		return mBounds;
	}

	public Color4f getColor() {
		return mColor;
	}

	public FloatBuffer getColorBuf() {
		return mColorBuf.getBuf(); 
	}

	public int getCullFace() { 
		return mCullFace; 
	}

	protected int getFrontFace() { 
		return GL10.GL_CCW; 
	}
	
	public ShortBuffer getIndexBuf() { 
		return mIndexBuf.getBuf(); 
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
		Bounds bounds = getBounds();
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(), bounds.getMidZ());
		Matrix4f matrix = getMatrix();
		matrix.multMV(midPoint);
		return midPoint;
	}

	public FloatBuffer getNormalBuf() { 
		return mNormalBuf.getBuf(); 
	}

	public Quaternion getQuaternionMod() { 
		return getMatrixMod().getQuaternion();
	}

	public FloatBuffer getTextureBuf() { 
		return mTextureBuf.getBuf(); 
	}

	public FloatBuffer getVertexBuf() {
		return mVertexBuf.getBuf(); 
	}

	public boolean hasColorArray() {
		return (mColorBuf.getBuf() != null);
	}

	public boolean hasNormalArray() {
		return (mNormalBuf.getBuf() != null);
	}

	public boolean hasTextureArray() {
		return (mTextureBuf.getBuf() != null);
	}

	public boolean hasVertexArray() {
		return (mVertexBuf.getBuf() != null);
	}

	public void onDraw(MatrixTrackingGL gl) {

		FloatBuffer fbuf;
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
		if ((fbuf = getVertexBuf()) != null) {
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fbuf);
		} else {
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
		if (mColorOutline != null) {
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		} else {
			if ((fbuf = getNormalBuf()) != null) {
				gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
				gl.glNormalPointer(GL10.GL_FLOAT, 0, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
			}
			if ((fbuf = getColorBuf()) != null) {
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
				gl.glColorPointer(4, GL10.GL_FLOAT, 0, fbuf);

				// Not doing it this way anymore:
				// gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
			} else {
				gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			}
			if ((mTexture != null) && ((fbuf = getTextureBuf()) != null)) {
				mTexture.onDraw(gl, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			}
		}
		ShortBuffer sbuf;
		if ((sbuf = getIndexBuf()) != null) {
			gl.glDrawElements(mIndexMode, sbuf.remaining(), GL10.GL_UNSIGNED_SHORT, sbuf);
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

	public void setLocation(Vector3f x) { 
		getMatrixMod().setLocation(x);
	}

	public void setMatrixMod(Matrix4f mod) {
		mMatrixMod = mod;
	}
	
	public void setIndexData(dShortBuf data) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
	}

	public void setIndexData(dShortBuf data, int mode) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
		mIndexMode = mode;
	}

	public void setNormalData(dFloatBuf data) {
		mNormalBuf = new FloatBuf();
		mNormalBuf.set(data);
	}

	public void setVertexData(dFloatBuf data) {
		mVertexBuf = new FloatBuf();
		mVertexBuf.set(data);
	}

}
