package com.tipsolutions.jacket.effect;

import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;

// Holds the entire collection of particles, their creation methods, 
// update methods, and cleanup methods.
public class ParticleSystem {
	
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
		if (mEmitter != null) {
			mEmitter.mTiming.mTimer.cancel();
		}
		mEmitter = emitter;
		mEmitter.setParticleSystem(this);
		mEmitter.init();
	}
	
	public void onCreate() {
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		if (mEmitter.mTiming.ready()) {
			mEmitter.mParticles.addParticles();
			mEmitter.mParticles.setLoc();
		}
		gl.glFrontFace(GL10.GL_CCW);
		gl.glDisable(GL10.GL_BLEND);
		
		Color4f color = mEmitter.mGeneralColor;
		
		if (color != null) {
			gl.glColor4f(color.getRed(), 
						 color.getGreen(), 
						 color.getBlue(), 
						 color.getAlpha());
		}
		{
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			
			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(mMatrix);
			if (mEmitter.getTexture() != null) {
    			useMatrix.setRotation(new Matrix3f());
			}
			gl.glLoadMatrix(useMatrix);
		}
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mEmitter.getVertexBuf());
		
		if (mEmitter.getTexture() != null) {
			mEmitter.getTexture().onDraw(gl, mEmitter.getTextureBuf());
			
//			ShortBuffer ibuf = mEmitter.getIndexBuf();
//    		gl.glDrawElements(GL10.GL_TRIANGLES, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
			
			for (int i = 0; i < mEmitter.getParticleCount(); i++) {
				ShortBuffer ibuf = mEmitter.getIndexBuf(i);
				gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT, ibuf);
			}
		} else {
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    		ShortBuffer ibuf = mEmitter.getIndexBuf();
    		gl.glDrawElements(GL10.GL_POINTS, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
		}
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
