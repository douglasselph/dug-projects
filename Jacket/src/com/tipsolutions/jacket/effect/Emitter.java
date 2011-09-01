package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.ShortBuf;

public class Emitter {
	
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
		
		public void init() {
			int size = (getCreatePerFrame() + getCreateVariance()) * getEnergyInitial();
			mList = new Particle[size];
			
			Particle sample = create();
			int bufSize = size * sample.getNumVertex();
			
			mVertexBuf = new FloatBuf();
			mVertexBuf.alloc(bufSize*3);
			mIndexBuf = new ShortBuf();
			mIndexBuf.alloc(bufSize);
			mVertexBuf.getBuf().limit(0);
			mIndexBuf.getBuf().limit(0);
		}
		
		public void addParticles() {
			int num = genCreateNum();
			for (int i = 0; i < mList.length; i++) {
				if (mList[i] == null) {
					mList[i] = create();
				} else if (!mList[i].isAlive()) {
					reinit(mList[i]);
				} else {
					continue;
				}
				if (--num <= 0) {
					break;
				}
			}
		}
		
		public void setAge() {
			// Set ages of all particles
			FloatBuffer vbuf = mVertexBuf.getBuf();
			ShortBuffer ibuf = mIndexBuf.getBuf();
			
			vbuf.limit(vbuf.capacity());
			ibuf.limit(ibuf.capacity());
			
			vbuf.rewind();
			ibuf.rewind();
			
			for (int i = 0; i < mList.length; i++) {
				Particle part = mList[i];
				if (part == null) {
					break;
				}
				if (part.isAlive()) {
					final int numVertex = part.getNumVertex();
					if (vbuf.position() + numVertex*3 < vbuf.capacity() &&
						ibuf.position() + numVertex < ibuf.capacity()) {
    					part.setAge(vbuf, i);
    					if (part.isAlive()) {
    						for (int j = 0; j < numVertex; j++) {
        						ibuf.put((short)(i+j));
    						}
    					}
					}
				}
			}
			vbuf.limit(vbuf.position());
			ibuf.limit(ibuf.position());
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
    					mParticleSystem.getView().requestRender();
    				}
    			}, mFrameIntervalMs);
    			mScheduled = true;
			}
		}
	};
	
	// The number of new particles to create per frame:
	protected int mCreatePerFrame;
	// The largest range of variance to add to mCreatePerFrame:
	protected int mCreateVariance;
	// Mid energy of newly created particle (age):
	protected int mMaxAgeInitial;
	// Amount of variance of starting energy level (age):
	protected int mMaxAgeInitialVariance;
	// Mid strength for setting initial velocity of particle (coords per frame):
	protected float mStrength;
	// Amount of variance of strength:
	protected float mStrengthVariance;
	// Control when each draw occurs
	protected Timing mTiming;
	// Associated ParticleSystem
	protected ParticleSystem mParticleSystem;
	// ColorTable
	protected ColorTable mColorTable = null;
	protected Color4f mGeneralColor = Color4f.BLACK;
	// Random generator 
	protected Random mRandom;
	// Common force applied to all particles
	protected Vector3f mForce = new Vector3f();
	protected Particles mParticles;
	
	public Emitter(int frameInterval,
				   int createPerFrame, int createVariance, 
				   int maxAgeInitial, int maxAgeVariance,
				   float strength, float strengthVariance) {
		mCreatePerFrame = createPerFrame;
		mCreateVariance = createVariance;
		mMaxAgeInitial = maxAgeInitial;
		mMaxAgeInitialVariance = maxAgeVariance;
		mStrength = strength;
		mStrengthVariance = strengthVariance;
		mTiming = new Timing(frameInterval);
		mParticles = new Particles();
		mRandom = new Random();
	}
	
	public void init() {
		mParticles.init();
	}
	
	public void setParticleSystem(ParticleSystem ps) {
		mParticleSystem = ps;
	}
	
	public void setRandomSeed(long seed) {
		mRandom = new Random(seed);
	}
	
	public void setRandom() {
		mRandom = new Random();
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
	
	public float getMaxDistance() {
		return (mStrength + mStrengthVariance) * (mMaxAgeInitial + mMaxAgeInitialVariance);
	}
}
