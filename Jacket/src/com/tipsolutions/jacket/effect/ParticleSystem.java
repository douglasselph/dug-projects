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
	
	public static Boolean DEBUG = false;
	public static Boolean DEBUG2 = false;
	public static Boolean DEBUG3 = false;
	
	// World location and rotation of particlar system
	protected Matrix4f mMatrix = new Matrix4f();
	final protected GLSurfaceView mView;
	// Used to customize the creation of particles in the system
	protected Emitter mEmitter;
	
	Object mLock = new Object();
	
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
	
	public void setEmitter(Emitter emitter) {
		synchronized (mLock) {
		if (mEmitter != null) {
			mEmitter.mTiming.stop();
		}
		mNumRuns = 0;
		mTotalTime = 0;
		mEmitter = emitter;
		mEmitter.setParticleSystem(this);
		mEmitter.init();
		mEmitter.mTiming.schedule();
		}
	}
	
	public void onCreate() {
		mEmitter.mTiming.reschedule();
	}
	
	public void onPause() {
		mEmitter.mTiming.stop();
	}
	
	public void onResume() {
		synchronized (mLock) {
			if (mEmitter != null) {
        		mEmitter.mTiming.reschedule();
			}
		}
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		synchronized (mLock) {
		if (DEBUG3)  {
			Log.d("DEBUG:", "---onDraw() START---");
		}
		mEmitter.mTiming.onDrawStart();
		if (mEmitter.mTiming.readyForCreate()) {
			mEmitter.mParticles.addParticles();
		}
		if (mEmitter.mTiming.readyForUpdate()) {
			mEmitter.mParticles.setLoc();
		}
		if (DEBUG3)  {
			Log.d("DEBUG:", "onDraw() DRAWING");
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
		if (DEBUG2) 
		{
			StringBuffer sbuf = new StringBuffer();
			FloatBuffer fbuf = mEmitter.getVertexBuf();
			while (fbuf.hasRemaining()) {
				sbuf.append(" ");
				sbuf.append(fbuf.get());
			}
			Log.d("DEBUG", "Vertex " + sbuf.toString());
		} else if (DEBUG3) {
			Log.d("DEBUG", "VERTEX " + mEmitter.getVertexBuf().limit());
		}

		FloatBuffer fbuf = mEmitter.getNormalBuf();
		if (fbuf != null) {
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, fbuf);
			
			if (DEBUG3) {
    			Log.d("DEBUG", "NORMAL " + fbuf.limit());
			}
		} else {
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
		boolean doIndividualPartColors = false;
		if (!mEmitter.hasParticleColors()) {
			Color4f color = mEmitter.getGeneralColor();

			if (color != null) {
				if (DEBUG2) {
    				Log.d("DEBUG", "Set general color");
				}
				gl.glColor4f(color.getRed(), 
						color.getGreen(), 
						color.getBlue(), 
						color.getAlpha());
			}
		} else {
			fbuf = mEmitter.getColorBuf();
			if (fbuf != null) {
				if (DEBUG2) {
    				Log.d("DEBUG", "Using color array");
				}
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
				gl.glColorPointer(4,GL10.GL_FLOAT, 0, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
				doIndividualPartColors = true;
			}
		}
		if (DEBUG3)  {
			Log.d("DEBUG:", "onDraw() DRAWING2");
		}
		if (mEmitter.getTexture() != null) {
			gl.setCullFace(0);
			mEmitter.getTexture().onDraw(gl, mEmitter.getTextureBuf());
			if (DEBUG2) 
			{
				StringBuffer sbuf = new StringBuffer();
				fbuf = mEmitter.getTextureBuf();
				while (fbuf.hasRemaining()) {
					sbuf.append(" ");
					sbuf.append(fbuf.get());
				}
				Log.d("DEBUG", "TEX " + sbuf.toString());
			}
			else if (DEBUG3) {
				fbuf = mEmitter.getTextureBuf();
				Log.d("DEBUG", "TEX " + fbuf.capacity() + ", " + fbuf.limit());
			}
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
				int keep = ibuf.remaining();
				if (DEBUG3)  {
					short maxVal = 0;
					short val;
					while (ibuf.hasRemaining()) {
						if ((val = ibuf.get()) > maxVal) {
							maxVal = val;
						}
					}
					ibuf.rewind();
					Log.d("DEBUG:", "onDraw() DRAWING3," + maxVal + ", " + ibuf.capacity() + ", " + ibuf.limit());
				}
				gl.glDrawElements(GL10.GL_TRIANGLES, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
				if (DEBUG3)  {
					Log.d("DEBUG:", "onDraw() DRAWING4");
				}
				if (DEBUG2) {
					StringBuffer sbuf = new StringBuffer();
					ibuf.rewind();
					while (ibuf.hasRemaining()) {
						sbuf.append(" ");
						sbuf.append(ibuf.get());
					}
					Log.d("DEBUG:", "Drawing " + keep + ", " + ibuf.limit() + ":" + sbuf.toString());
				}
			}
		} else {
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

			ShortBuffer ibuf = mEmitter.getIndexBuf();
			gl.glDrawElements(GL10.GL_POINTS, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
		}
		gl.glPopMatrix();

		if (DEBUG) {
			long diff = System.currentTimeMillis() - mEmitter.mTiming.mCurTime;
			mTotalTime += diff;
			mNumRuns++;
			
			if (mNumRuns % 100 == 0) {
    			Log.d("DEBUG", "Trys=" + mNumRuns + ", avTime=" +mTotalTime/mNumRuns + "ms");
			}
		}
		mEmitter.mTiming.schedule();
		
		if (DEBUG3)  {
			Log.d("DEBUG:", "---onDraw() DONE--- " + mEmitter.getParticleCount());
		}
		}
	}
	
	public void addRotate(double angleX, double angleY, double angleZ) {
		synchronized (mLock) {
			mMatrix.addRotate(angleX, angleY, angleZ);
		}
	}

	public Matrix4f getMatrix() {
		return mMatrix;
	}
	
	public float getMaxDistance() {
		return mEmitter.getMaxDistance();
	}
	
	public GLSurfaceView getView() {
		return mView;
	}
	
}