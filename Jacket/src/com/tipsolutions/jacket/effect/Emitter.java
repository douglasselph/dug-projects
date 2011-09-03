package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.ShortBuf;

public class Emitter {
	
	protected class ColorTable {
		ArrayList<Color4f> mList = new ArrayList<Color4f>();
		
		public void clear() {
			mList.clear();
		}
		
		public void setSize(int size) {
			mList.clear();
			mList.ensureCapacity(size);
			
			Color4f diffColor = new Color4f(
					mEndColor.getRed() - mStartColor.getRed(),
					mEndColor.getGreen() - mStartColor.getGreen(),
					mEndColor.getBlue() - mStartColor.getBlue(),
					mEndColor.getAlpha() - mStartColor.getAlpha());
			
			float ratio;
			
			for (int i = 0; i < size; i++) {
				ratio = (float)i/(float)(size-1);
				mList.add(new Color4f(
						mStartColor.getRed()+diffColor.getRed()*ratio,
						mStartColor.getGreen()+diffColor.getGreen()*ratio,
						mStartColor.getBlue()+diffColor.getBlue()*ratio,
						mStartColor.getAlpha()+diffColor.getAlpha()*ratio));
			}
		}
		
		public Color4f getColor(int index) {
			if (index >= mList.size()) {
				index = mList.size()-1;
			}
			return mList.get(index);
		}
	}
	
	// By default the base class represents a simple point
	protected class Particle {
		protected Vector3f mLastPos;
		protected Vector3f mVelocity;
		protected short mMaxAge;
		protected short mAge;
		
		public Particle(Vector3f velocity, short maxAge) {
			reinit(velocity, maxAge);
		}
		
		public Color4f getColor() {
			return mColorTable.getColor(mAge);
		}
		
		protected Vector3f getLoc(short age) {
			tempVec.setX((mVelocity.getX()+mForce.getX())*age);
			tempVec.setY((mVelocity.getY()+mForce.getY())*age);
			tempVec.setZ((mVelocity.getZ()+mForce.getZ())*age);
			return tempVec;
		}
		
		public short getINum() {
			return 1;
		}
		
		public short getVNum() {
			return 1;
		}
		
		public int getVPos(int index) {
			return index*3*getVNum();
		}
		
		public boolean isAlive() {
			return (mAge <= mMaxAge);
		}
		
		public void reinit(Vector3f velocity, short maxAge) {
			mLastPos = null;
			mVelocity = velocity;
			mMaxAge = maxAge;
			mAge = 0;
		}
		
		public boolean setLoc(int index) {
			if (mAge > mMaxAge) {
				return false;
			}
			Vector3f loc = getLoc(mAge);
			int vpos = index*3;
			mVertexFbuf.position(vpos);
			mVertexFbuf.put(loc.getX()).put(loc.getY()).put(loc.getZ());

			mIndexSbuf.put((short) index);
			return true;
		}
	};
	
	protected class Particles {
		// List of particles
		protected Particle [] mList;
		protected short vNum;
		protected short iNum;
		
		public void addParticles() {
			int num = genCreateNum();
			if (num > 0) {
    			for (int i = 0; i < mList.length; i++) {
    				if (mList[i] == null) {
    					mList[i] = createParticle();
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
		}
		
//		boolean checkAdd(int numVertex) {
//			boolean flag = true;
//			if (mVertexFbuf.position() + numVertex*3 >= mVertexFbuf.capacity()) {
//				Msg.err("Too many vertexes: " + mVertexFbuf.position() + "+3*" + numVertex + ">=" + mVertexFbuf.capacity());
//				flag = false;
//			}
//			if (mIndexSbuf.position() + numVertex >= mIndexSbuf.capacity()) {
//				Msg.err("Too many indexes: " + mIndexSbuf.position() + "+" + numVertex + ">=" + mIndexSbuf.capacity());
//				flag = false;
//			}
//			return flag;
//		}
		
		public Particle createParticle() {
			return new Particle(genVelocity(), genMaxAge());
		}

		public void init() {
			long maxLifeMs = (mMaxAge + mMaxAgeVar) * mAgeInterval;
			float maxNumCreatesPerLife = (float)maxLifeMs / (float)mCreateInterval;
			int size = (int) ((mCreateCount + mCreateVar) * maxNumCreatesPerLife);
			
			mList = new Particle[size];
			
			Particle sample = createParticle();
			vNum = sample.getVNum();
			iNum = sample.getINum();
			
			int bufSize = mParticles.mList.length * vNum;
			
			mVertexBuf = new FloatBuf();
			mVertexBuf.alloc(bufSize*3);
			mVertexBuf.getBuf().limit(0);
			
			bufSize = mParticles.mList.length * iNum;
			mIndexBuf = new ShortBuf();
			mIndexBuf.alloc(bufSize);
			mIndexBuf.getBuf().limit(0);
			
			mNumAlive = 0;
			
			if (hasParticleColors()) {
				mColorTable.setSize(mMaxAge + mMaxAgeVar);
			} else {
				mColorTable.clear();
			}
		}
		
		public void setLoc() {
			// Set ages of all particles
			mVertexFbuf = mVertexBuf.getBuf();
			mIndexSbuf = mIndexBuf.getBuf();

			mVertexFbuf.limit(mVertexFbuf.capacity());
			mIndexSbuf.limit(mIndexSbuf.capacity());

			mVertexFbuf.rewind();
			mIndexSbuf.rewind();
			
			mNumAlive = 0;
			
//			Log.d("DEBUG", "mList size=" + mList.length + ", vertex cap=" + mVertexFbuf.limit());
			
			for (int i = 0; i < mList.length; i++) {
				Particle part = mList[i];
				if (part == null) {
					break;
				}
				if (part.isAlive()) {
					if (part.setLoc(i)) {
						part.mAge++;
						mNumAlive++;
					}
				}
			}
			mVertexFbuf.limit(mVertexFbuf.position());
			mIndexSbuf.limit(mIndexSbuf.position());
		}
	}
	
	protected class Timing {
		protected long mCurTime;
		protected long mLastUpdate = 0;
		protected long mLastCreate = 0;
		protected Timer mTimer = new Timer();
		protected Boolean mScheduled = false;
		
		public Timing() {
		}
		
		public long getCurTime() {
			return mCurTime;
		}
		
		void stop() {
			if (mTimer != null) {
    			mTimer.cancel();
    			mTimer = new Timer();
			}
			mScheduled = false;
			mLastCreate = 0;
			mLastUpdate = 0;
		}
		
		void onDrawStart() {
			mCurTime = System.currentTimeMillis();
		}
		
		boolean readyForCreate() {
			long diff = mCurTime - mLastCreate;
			if (diff >= mCreateInterval) {
				mLastCreate = mCurTime;
				return true;
			}
			return false;
		}
		
		boolean readyForUpdate() {
			long diff = mCurTime - mLastUpdate;
			if (diff >= mAgeInterval) {
				mLastUpdate = mCurTime;
				return true;
			}
			return false;
		}
		
		void reschedule() {
			stop();
			schedule();
		}
		
		synchronized void schedule() {
			if (!mScheduled) {
    			long diffC = mCreateInterval - (mCurTime - mLastCreate);
    			long diffU = mAgeInterval - (mCurTime - mLastUpdate);
    			long diff;
    			
    			if (diffC < diffU) {
    				diff = diffC;
    			} else {
    				diff = diffU;
    			}
    			if (diff <= 0) {
    				mParticleSystem.getView().requestRender();
    			} else {
        			mTimer.schedule(new TimerTask() {
        				@Override
        				public void run() {
        					activity();
        				}
        			}, diff);
        			mScheduled = true;
    			}
			}
		}
		
		synchronized void activity() {
			mParticleSystem.getView().requestRender();
			mScheduled = false;
		}
	};
	
	static protected Vector3f tempVec = new Vector3f();
	
	// The number of new particles to create per designated interval
	protected short mCreateCount = 30;
	// How often to create 
	protected short mCreateInterval = 30;
	// The largest range of variance to add to mCreatePerFrame:
	protected short mCreateVar = 5;
	// Mid energy of newly created particle (age):
	protected short mMaxAge = 100;
	// Amount of variance of starting energy level (age):
	protected short mMaxAgeVar = 10;
	// How often to age particles
	protected short mAgeInterval = 30;
	// Mid strength for setting initial velocity of particle (coords per frame):
	protected float mStrength = 0.05f;
	// Amount of variance of strength:
	protected float mStrengthVar = 0.01f;
	// Control when each draw occurs
	protected Timing mTiming = new Timing();
	// Associated ParticleSystem
	protected ParticleSystem mParticleSystem;
	// Colors
	protected Color4f mStartColor = Color4f.BLACK;
	protected Color4f mEndColor = null;
	protected ColorTable mColorTable = new ColorTable();
	// Random generator 
	protected Random mRandom;
	// Common force applied to all particles
	protected Vector3f mForce = new Vector3f();
	protected Particles mParticles;
	protected int mNumAlive = 0;
	
	protected FloatBuf mVertexBuf;
	protected FloatBuffer mVertexFbuf;
	
	protected ShortBuf mIndexBuf;
	protected ShortBuffer mIndexSbuf;
	
	public Emitter() {
		mTiming = new Timing();
		mParticles = new Particles();
		mRandom = new Random();
	}
	
	public int genCreateNum() {
		return mCreateCount + mRandom.nextInt(mCreateVar*2+1) - mCreateVar;
	}
	
	protected short genMaxAge() {
		return (short) (mMaxAge + mRandom.nextInt(mMaxAgeVar*2+1) - mMaxAgeVar);
	}
	
	protected Vector3f genVelocity() {
		float strength = mStrength + (mStrengthVar * mRandom.nextFloat() * 2) - mStrengthVar;
		Vector3f vec = new Vector3f(mRandom.nextFloat()*2-1,
								    mRandom.nextFloat()*2-1,
								    mRandom.nextFloat()*2-1);
		vec.normalize();
		return vec.multiply(strength);
	}
	
	public Color4f getGeneralColor() {
		if (mStartColor == null && mEndColor == null) {
			return null;
		}
		if (mStartColor == null) {
			return mEndColor;
		}
		if (mEndColor == null) {
			return mStartColor;
		}
		if (mStartColor.equals(mEndColor)) {
			return mStartColor;
		}
		return null;
	}
	
	public boolean hasParticleColors() {
		return mStartColor != null && mEndColor != null;
	}
	
	public short getCreateInterval() { return mCreateInterval; }
	public short getCreateCount() { return mCreateCount; }
	public short getCreateVar() { return mCreateVar; }
	public Color4f getStartColor() { return mStartColor; }
	public Color4f getEndColor() { return mEndColor; }
	public short getMaxAge() { return mMaxAge; }
	public short getMaxAgeVar() { return mMaxAgeVar; }
	public short getAgeInterval() { return mAgeInterval; }
	public float getStrength() { return mStrength; }
	public float getStrengthVar() { return mStrengthVar; }
	
	public ShortBuffer getIndexBuf() { return mIndexBuf.getBuf(); }
	public ShortBuffer getIndexBuf(int i) { return null; }
	
	public float getMaxDistance() {
		return (mStrength + mStrengthVar) * (mMaxAge + mMaxAgeVar);
	}
	
	public Color4f getParticleColor(int i) {
		return mParticles.mList[i].getColor();
	}
	
	public FloatBuffer getColorBuf() { return null; }
	public FloatBuffer getNormalBuf() { return null; }
	public int getParticleCount() { return mNumAlive; }
	public Texture getTexture() { return null; }
	public FloatBuffer getTextureBuf() { return null; }
	public FloatBuffer getVertexBuf() { return mVertexBuf.getBuf(); }
	
	public void init() {
		mParticles.init();
	}
	
	public void reinit(Particle part) {
		part.reinit(genVelocity(), genMaxAge());
	}
	
	public void setCreate(short createInterval, short numToCreate, short createVar) {
		mCreateInterval = createInterval; 
		mCreateCount = numToCreate;
		mCreateVar = createVar;
	}
	
	public void setGeneralColor(Color4f color) {
		mStartColor = color;
		mEndColor = null;
	}
	
	public void setStartColor(Color4f color) { mStartColor = color; }
	public void setEndColor(Color4f color) { mEndColor = color; }
	
	public void setAge(short ageInterval, short maxAge, short maxAgeVar) {
		mAgeInterval = ageInterval;
		mMaxAge = maxAge;
		mMaxAgeVar = maxAgeVar;
	}
	public void setStrength(float strength, float var) { 
		mStrength = strength; 
		mStrengthVar = var;
	}
	public void setParticleSystem(ParticleSystem ps) { mParticleSystem = ps; }
	public void setRandom() { mRandom = new Random(); }
	public void setRandomSeed(long seed) { mRandom = new Random(seed); }
}
