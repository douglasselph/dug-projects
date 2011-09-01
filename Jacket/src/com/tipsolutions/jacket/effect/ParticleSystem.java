package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.ShortBuf;

// Holds the entire collection of particles, their creation methods, 
// update methods, and cleanup methods.
public class ParticleSystem {
	
	public class ColorTable {
		ArrayList<Color4f> mColors = new ArrayList<Color4f>();
		
		public int numColors() { return mColors.size(); }
		
		public Color4f getColor(int age) {
			return mColors.get(age % mColors.size());
		}
	};
	
	// By default the base class represents a simple point
	public class Particle {
		protected Vector3f mLastPos;
		protected Vector3f mVelocity;
		protected long mBirthtime; // time particle was born
		protected int mMaxAge;
		protected boolean mDoColor; // Lookup in color table.
		
		public Particle(Vector3f velocity, boolean doColor) {
			mLastPos = null;
			mDoColor = doColor;
			mVelocity = velocity;
			mBirthtime = mTiming.getCurTime();
		}
		
		public Particle(Vector3f velocity, int maxAge) {
			reinit(velocity, maxAge);
		}
		
		public void reinit(Vector3f velocity, int maxAge) {
			mLastPos = null;
			mVelocity = velocity;
			mDoColor = false;
			mBirthtime = mTiming.getCurTime();
			mMaxAge = maxAge;
		}
		
		public int getAge() {
			return mTiming.getAge(mBirthtime);
		}
		
		public Color4f getColor() {
			if (mDoColor) {
				mColorTable.getColor(getAge());
			}
			return mGeneralColor;
		}
		
		public void setAge(FloatBuffer buf, int index) {
			int age = getAge();
			
			if (age <= mMaxAge) {
    			float x = (mVelocity.getX()+mForce.getX())*age;
    			float y = (mVelocity.getY()+mForce.getY())*age;
    			float z = (mVelocity.getZ()+mForce.getZ())*age;
    			
				int pos = index*3;
    			buf.position(pos);
    			buf.put(x).put(y).put(z);
			} else {
				mMaxAge = 0;
			}
		}
		
		public boolean isAlive() {
			return mMaxAge > 0;
		}
		
		public int getNumVertex() {
			return 1;
		}
	};
	
	protected class Particles {
		// List of particles
		protected Particle [] mList;
		protected FloatBuf mVertexBuf;
		protected ShortBuf mIndexBuf;
		protected long mLastAddParticleTime; // Last time we added particles
		
		public void init() {
			int size = (mEmitter.getCreatePerFrame() + mEmitter.getCreateVariance()) * mEmitter.getEnergyInitial();
			mList = new Particle[size];
			
			int num = mEmitter.genCreateNum();
			Particle sample = mEmitter.create();
			int bufSize = size * sample.getNumVertex();
			
			mVertexBuf = new FloatBuf();
			mIndexBuf = new ShortBuf();
			mVertexBuf.alloc(bufSize*3);
			ShortBuffer ibuf = mIndexBuf.alloc(bufSize);
			
			for (int i = 0; i < num; i++) {
				mList[i] = mEmitter.create();
				ibuf.put((short)i);
			}
			ibuf.limit(num);
			
			mVertexBuf.getBuf().limit(num*3);
			
			mLastAddParticleTime = System.currentTimeMillis();
		}
		
		public void setAge() {
			// Determine number of new particles to create
			int times = mTiming.getAge(mLastAddParticleTime);
			int num = 0;
			for (int t = 0; t < times; t++) {
    			num += mEmitter.genCreateNum();
			}
			if (num > 0) {
				// Create new particles
    			for (int i = 0; i < mList.length; i++) {
    				if (mList[i] == null) {
    					mList[i] = mEmitter.create();
    				} else if (!mList[i].isAlive()) {
    					mEmitter.reinit(mList[i]);
    				}
    			}
			}
			mLastAddParticleTime = mTiming.getCurTime();
			
			// Set ages of all particles
			FloatBuffer vbuf = mVertexBuf.getBuf();
			ShortBuffer ibuf = mIndexBuf.getBuf();
			
			vbuf.limit(vbuf.capacity());
			ibuf.limit(ibuf.capacity());
			
			vbuf.rewind();
			ibuf.rewind();
			
			for (int i = 0; i < mList.length; i++) {
				if (mList[i] == null) {
					break;
				}
				if (mList[i].isAlive()) {
					mList[i].setAge(vbuf, i);
					
					if (mList[i].isAlive()) {
						ibuf.put((short)i);
					}
				}
			}
			vbuf.limit(vbuf.position());
			ibuf.limit(ibuf.position());
		}
	};
	
	// A base class Emitter which creates particles that explode in all directions.
	public class Emitter {
		// The number of new particles to create per frame:
		int mCreatePerFrame;
		// The largest range of variance to add to mCreatePerFrame:
		int mCreateVariance;
		// Mid energy of newly created particle (age):
		int mMaxAgeInitial;
		// Amount of variance of starting energy level (age):
		int mMaxAgeInitialVariance;
		// Mid strength for setting initial velocity of particle (coords per frame):
		float mStrength;
		// Amount of variance of strength:
		float mStrengthVariance;
		
		public Emitter(int createPerFrame, int createVariance, 
					   int maxAgeInitial, int maxAgeVariance,
					   float strength, float strengthVariance) {
			mCreatePerFrame = createPerFrame;
			mCreateVariance = createVariance;
			mMaxAgeInitial = maxAgeInitial;
			mMaxAgeInitialVariance = maxAgeVariance;
			mStrength = strength;
			mStrengthVariance = strengthVariance;
		}
		
		// Note: the particles location are taken relative to the system.
		public Particle create() {
			return new Particle(genVelocity(), genMaxAge());
		}
		
		public void reinit(Particle part) {
			part.reinit(genVelocity(), genMaxAge());
		}
		
		public int getCreatePerFrame() { return mCreatePerFrame; }
		public int getCreateVariance() { return mCreateVariance; }
		public int getEnergyInitial() { return mMaxAgeInitial; }
		
		public int genCreateNum() {
			return mCreatePerFrame + mRandom.nextInt(mCreateVariance*2) - mCreateVariance;
		}
		
		protected int genMaxAge() {
			return mMaxAgeInitial + mRandom.nextInt(mMaxAgeInitialVariance*2) - mMaxAgeInitialVariance;
		}
		
		protected Vector3f genVelocity() {
			float strength = mStrength + (mStrengthVariance * mRandom.nextFloat() * 2) - mStrengthVariance;
			Vector3f vec = new Vector3f(mRandom.nextFloat()*2-1,
									    mRandom.nextFloat()*2-1,
									    mRandom.nextFloat()*2-1);
			vec.normalize();
			return vec.multiply(strength);
		}
	};
	
	protected class Timing {
		protected long mLastDraw; // Time of last draw
		protected long mCurTime;  // This time
		protected int mFrameIntervalMs; // Number of milliseconds per frame
		protected boolean mScheduled = false;
		
		Timer timer = new Timer();
		
		public Timing(int intervalMs) {
			mFrameIntervalMs = intervalMs;
		}
		
		public long getCurTime() {
			return mCurTime;
		}
		
		public int getAge(long startTime) {
			return (int) (mCurTime - startTime) / mFrameIntervalMs;
		}
		
		boolean ready() {
			mCurTime = System.currentTimeMillis();
			return (mCurTime >= mLastDraw + mFrameIntervalMs);
		}
		
		void schedule() {
			mLastDraw = mCurTime;
			
			if (!mScheduled) {
    			timer.schedule(new TimerTask() {
    				@Override
    				public void run() {
    					mScheduled = false;
    					mView.requestRender();
    				}
    			}, mFrameIntervalMs);
    			mScheduled = true;
			}
		}
	};
	
	// Particle management
	protected Particles mParticles = new Particles();
	// Shared texture used to render each particle
	protected Texture mTexture = null;
	// Common force applied to all particles
	protected Vector3f mForce;
	// World location and rotation of particlar system
	protected Matrix4f mMatrix = new Matrix4f();
	// Used to lookup colors used in particles;
	protected ColorTable mColorTable = null;
	protected Color4f mGeneralColor = null;
	// Random generator 
	protected Random mRandom;
	// Used to control the rate of frame draws
	final protected Timing mTiming;
	final protected GLSurfaceView mView;
	// Used to customize the creation of particles in the system
	protected Emitter mEmitter;
	
	public ParticleSystem(GLSurfaceView view, int frameIntervalMs) {
		mView = view;
		mRandom = new Random();
		mTiming = new Timing(frameIntervalMs);
	}
	
	public ParticleSystem(GLSurfaceView view, long seed, int frameIntervalMs) {
		mView = view;
		mRandom = new Random(seed);
		mTiming = new Timing(frameIntervalMs);
	}
	
	public void setEmitter(Emitter emitter) {
		mEmitter = emitter;
		mParticles.init();
	}
	
	public void setRandomSeed(long seed) {
		mRandom = new Random(seed);
	}
	
	public void onCreate() {
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		if (mTiming.ready()) {
			mParticles.setAge();
		}
		{
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();

			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(mMatrix);
			gl.glLoadMatrix(useMatrix);
		}
		gl.glDisable(GL10.GL_BLEND);
		
		if (mGeneralColor != null) {
			gl.glColor4f(mGeneralColor.getRed(), 
						 mGeneralColor.getGreen(), 
						 mGeneralColor.getBlue(), 
						 mGeneralColor.getAlpha());
		}
		if (mTexture != null) {
			mTexture.onDraw(gl);
		}
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mParticles.mVertexBuf.getBuf());
		
		ShortBuffer ibuf = mParticles.mIndexBuf.getBuf();
		gl.glDrawElements(GL10.GL_POINTS, ibuf.remaining(), GL10.GL_UNSIGNED_SHORT, ibuf);
		
		gl.glPopMatrix();
		
		mTiming.schedule();
	}
	
	public Matrix4f getMatrix() {
		return mMatrix;
	}
	
}
