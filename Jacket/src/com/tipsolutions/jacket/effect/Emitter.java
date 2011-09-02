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
		protected long mBirthtime; // time particle was born
		protected int mMaxAge;
		
		public Particle(Vector3f velocity, int maxAge) {
			reinit(velocity, maxAge);
		}
		
		public int getAge() {
			return mTiming.getAge(mBirthtime);
		}
		
		public Color4f getColor() {
			return mColorTable.getColor(getAge());
		}
		
		protected Vector3f getLoc(int age) {
			tempVec.setX((mVelocity.getX()+mForce.getX())*age);
			tempVec.setY((mVelocity.getY()+mForce.getY())*age);
			tempVec.setZ((mVelocity.getZ()+mForce.getZ())*age);
			return tempVec;
		}
		
		public short getVNum() {
			return 1;
		}
		
		public short getINum() {
			return 1;
		}
		
		public int getVPos(int index) {
			return index*3*getVNum();
		}
		
		public boolean isAlive() {
			return mMaxAge > 0;
		}
		
		public void reinit(Vector3f velocity, int maxAge) {
			mLastPos = null;
			mVelocity = velocity;
			mBirthtime = mTiming.getCurTime();
			mMaxAge = maxAge;
		}
		
		public boolean setLoc(int index) {
			int age = getAge();
			
			if (age <= mMaxAge) {
    			Vector3f loc = getLoc(age);
    			int vpos = getVPos(index);
    			mVertexFbuf.position(vpos);
    			mVertexFbuf.put(loc.getX()).put(loc.getY()).put(loc.getZ());
    			
    			mIndexSbuf.put((short)vpos);
			} else {
				mMaxAge = 0;
			}
			return (mMaxAge > 0);
		}
	};
	
	protected class Particles {
		// List of particles
		protected Particle [] mList;
		protected short vNum;
		protected short iNum;
		
		public void addParticles() {
			int num = genCreateNum();
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
			int size = (getCreatePerFrame() + getCreateVar()) * getMaxAge();
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
			
			for (int i = 0; i < mList.length; i++) {
				Particle part = mList[i];
				if (part == null) {
					break;
				}
				if (part.isAlive()) {
//					final int nVertex = part.getNumVertex();
//					if (DEBUG) {
//						if (!checkAdd(part.get)) {
//							continue;
//						}
//					}
					if (part.setLoc(i)) {
						mNumAlive++;
					}
				}
			}
			mVertexFbuf.limit(mVertexFbuf.position());
			mIndexSbuf.limit(mIndexSbuf.position());
		}
	}
	
	protected class Timing {
		protected long mLastDraw; // Time of last draw
		protected long mCurTime;  // This time
		protected int mFrameIntervalMs; // Number of milliseconds per frame
		protected boolean mScheduled = false;
		protected Timer mTimer = new Timer();
		
		public Timing(int intervalMs) {
			mFrameIntervalMs = intervalMs;
		}
		
		public int getAge(long startTime) {
			return (int) (mCurTime - startTime) / mFrameIntervalMs;
		}
		
		public long getCurTime() {
			return mCurTime;
		}
		
		boolean ready() {
			mCurTime = System.currentTimeMillis();
			return (mCurTime >= mLastDraw + mFrameIntervalMs);
		}
		
		void schedule() {
			mLastDraw = mCurTime;
			
			if (!mScheduled) {
    			mTimer.schedule(new TimerTask() {
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
	
	static protected Vector3f tempVec = new Vector3f();
	
	// The number of new particles to create per frame:
	protected int mCreatePerFrame = 30;
	// The largest range of variance to add to mCreatePerFrame:
	protected int mCreateVar = 5;
	// Mid energy of newly created particle (age):
	protected int mMaxAge = 100;
	// Amount of variance of starting energy level (age):
	protected int mMaxAgeVar = 10;
	// Mid strength for setting initial velocity of particle (coords per frame):
	protected float mStrength = 0.05f;
	// Amount of variance of strength:
	protected float mStrengthVar = 0.01f;
	// Control when each draw occurs
	protected Timing mTiming;
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
		mTiming = new Timing(30);
		mParticles = new Particles();
		mRandom = new Random();
	}
	
	public int genCreateNum() {
		return mCreatePerFrame + mRandom.nextInt(mCreateVar*2+1) - mCreateVar;
	}
	
	protected int genMaxAge() {
		return mMaxAge + mRandom.nextInt(mMaxAgeVar*2+1) - mMaxAgeVar;
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
	
	public int getCreatePerFrame() { return mCreatePerFrame; }
	public int getCreateVar() { return mCreateVar; }
	public int getFrameIntervalMs() { return mTiming.mFrameIntervalMs; }
	public Color4f getStartColor() { return mStartColor; }
	public Color4f getEndColor() { return mEndColor; }
	public int getMaxAge() { return mMaxAge; }
	public int getMaxAgeVar() { return mMaxAgeVar; }
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
	
	public void setCreatePerFrame(int v) { mCreatePerFrame = v; }
	public void setCreateVar(int v) { mCreateVar = v; }
	public void setFrameIntervalMs(int v) { mTiming.mFrameIntervalMs = v; }
	
	public void setGeneralColor(Color4f color) {
		mStartColor = color;
		mEndColor = null;
	}
	
	public void setStartColor(Color4f color) { mStartColor = color; }
	public void setEndColor(Color4f color) { mEndColor = color; }
	public void setMaxAge(int v) { mMaxAge = v; }
	public void setMaxAgeVar(int v) { mMaxAgeVar = v; }
	public void setStrength(float v) { mStrength = v; }
	public void setStrengthVar(float v) { mStrengthVar = v; }
	
	public void setParticleSystem(ParticleSystem ps) { mParticleSystem = ps; }
	public void setRandom() { mRandom = new Random(); }
	public void setRandomSeed(long seed) { mRandom = new Random(seed); }
}
