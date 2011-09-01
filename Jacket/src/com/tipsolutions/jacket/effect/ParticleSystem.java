package com.tipsolutions.jacket.effect;

import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Vector3f;

// Holds the entire collection of particles, their creation methods, 
// update methods, and cleanup methods.
public class ParticleSystem {
	
	// Shared texture used to render each particle
	protected Texture mTexture = null;
	// Common force applied to all particles
	protected Vector3f mForce = new Vector3f();
	// World location and rotation of particlar system
	protected Matrix4f mMatrix = new Matrix4f();
	final protected GLSurfaceView mView;
	// Used to customize the creation of particles in the system
	protected Emitter mEmitter;
	
	public ParticleSystem(GLSurfaceView view, Emitter emitter) {
		mView = view;
		setEmitter(emitter);
	}
	
	public ParticleSystem(GLSurfaceView view) {
		mView = view;
	}
	
	public void setEmitter(Emitter emitter) {
		mEmitter = emitter;
		mEmitter.setParticleSystem(this);
		mEmitter.init();
	}
	
	public void onCreate() {
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		if (mEmitter.mTiming.ready()) {
			mEmitter.mParticles.addParticles();
			mEmitter.mParticles.setAge();
		}
		{
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();

			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(mMatrix);
			gl.glLoadMatrix(useMatrix);
		}
		gl.glDisable(GL10.GL_BLEND);
		
		Color4f color = mEmitter.mGeneralColor;
		
		if (color != null) {
			gl.glColor4f(color.getRed(), 
						 color.getGreen(), 
						 color.getBlue(), 
						 color.getAlpha());
		}
		if (mTexture != null) {
			mTexture.onDraw(gl);
		}
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mEmitter.mParticles.mVertexBuf.getBuf());
		
		ShortBuffer ibuf = mEmitter.mParticles.mIndexBuf.getBuf();
		gl.glDrawElements(GL10.GL_POINTS, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
		
		gl.glPopMatrix();
		
		mEmitter.mTiming.schedule();
	}
	
	public Matrix4f getMatrix() {
		return mMatrix;
	}
	
	public float getMaxDistance() {
		return mEmitter.getMaxDistance();
	}
	
	public int getFrameIntervalMs() {
		return mEmitter.mTiming.mFrameIntervalMs;
	}
	
	public GLSurfaceView getView() {
		return mView;
	}
	
}
