package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
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
	
	static final Boolean DEBUG = true;
	
	// World location and rotation of particlar system
	protected Matrix4f mMatrix = new Matrix4f();
	final protected GLSurfaceView mView;
	// Used to customize the creation of particles in the system
	protected Emitter mEmitter;
	
	// Debugging:
	protected long mTotalTime = 0;
	protected int mNumRuns = 0;
	
	public ParticleSystem(GLSurfaceView view, Emitter emitter) {
		mView = view;
		setEmitter(emitter);
	}
	
	public ParticleSystem(GLSurfaceView view) {
		mView = view;
	}
	
	public synchronized void setEmitter(Emitter emitter) {
		if (mEmitter != null) {
			mEmitter.mTiming.mTimer.cancel();
		}
		mNumRuns = 0;
		mTotalTime = 0;
		mEmitter = emitter;
		mEmitter.setParticleSystem(this);
		mEmitter.init();
	}
	
	public void onCreate() {
		mEmitter.mTiming.schedule();
	}
	
	public synchronized void onDraw(MatrixTrackingGL gl) {
		if (mEmitter.mTiming.ready()) {
			mEmitter.mParticles.addParticles();
			mEmitter.mParticles.setLoc();
		}
		gl.glFrontFace(GL10.GL_CW);
		gl.glDisable(GL10.GL_BLEND);
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

		FloatBuffer fbuf = mEmitter.getNormalBuf();
		if (fbuf != null) {
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, fbuf);
		} else {
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
		boolean doIndividualPartColors = false;
		if (!mEmitter.hasParticleColors()) {
			Color4f color = mEmitter.getGeneralColor();

			if (color != null) {
				gl.glColor4f(color.getRed(), 
						color.getGreen(), 
						color.getBlue(), 
						color.getAlpha());
			}
		} else {
			fbuf = mEmitter.getColorBuf();
			if (fbuf != null) {
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
				gl.glColorPointer(4,GL10.GL_FLOAT, 0, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
				doIndividualPartColors = true;
			}
		}
		if (mEmitter.getTexture() != null) {
			gl.setCullFace(0);
			mEmitter.getTexture().onDraw(gl, mEmitter.getTextureBuf());

			if (EmitterTex.USE_STRIPS) { 
				Color4f color;
				for (int i = 0; i < mEmitter.getParticleCount(); i++) {
					if (doIndividualPartColors) {
						color = mEmitter.getParticleColor(i);
						gl.glColor4f(color.getRed(), 
								color.getGreen(), 
								color.getBlue(), 
								color.getAlpha());
					}
					ShortBuffer ibuf = mEmitter.getIndexBuf(i);
					gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT, ibuf);
				}
			} else {
				ShortBuffer ibuf = mEmitter.getIndexBuf();
				gl.glDrawElements(GL10.GL_TRIANGLES, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
			}
		} else {
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

			ShortBuffer ibuf = mEmitter.getIndexBuf();
			gl.glDrawElements(GL10.GL_POINTS, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
		}
		gl.glPopMatrix();

//		if (DEBUG) {
//			long diff = System.currentTimeMillis() - mEmitter.mTiming.mCurTime;
//			mTotalTime += diff;
//			mNumRuns++;
//			Log.d("DEBUG", "Trys=" + mNumRuns + ", avTime=" +mTotalTime/mNumRuns + "ms");
//		}
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
