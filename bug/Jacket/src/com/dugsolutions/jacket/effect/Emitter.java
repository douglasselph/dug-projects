package com.dugsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.dugsolutions.jacket.image.Texture;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.shape.BufferUtils.FloatBuf;
import com.dugsolutions.jacket.shape.BufferUtils.ShortBuf;

public class Emitter
{

	protected class ColorTable
	{
		ArrayList<Color4f>	mList	= new ArrayList<Color4f>();

		public void clear()
		{
			mList.clear();
		}

		public Color4f getColor(int index)
		{
			if (index >= mList.size())
			{
				index = mList.size() - 1;
			}
			return mList.get(index);
		}

		public void setSize(int size)
		{
			mList.clear();
			mList.ensureCapacity(size);

			Color4f diffColor = new Color4f(mEndColor.getRed() - mStartColor.getRed(), mEndColor.getGreen()
					- mStartColor.getGreen(), mEndColor.getBlue() - mStartColor.getBlue(), mEndColor.getAlpha()
					- mStartColor.getAlpha());

			float ratio;

			for (int i = 0; i < size; i++)
			{
				ratio = (float) i / (float) (size - 1);
				mList.add(new Color4f(mStartColor.getRed() + diffColor.getRed() * ratio, mStartColor.getGreen()
						+ diffColor.getGreen() * ratio, mStartColor.getBlue() + diffColor.getBlue() * ratio,
						mStartColor.getAlpha() + diffColor.getAlpha() * ratio));
			}
		}
	}

	// By default the base class represents a simple point
	protected class Particle
	{
		protected Vector3f	mLastPos;
		protected Vector3f	mVelocity;
		protected short		mMaxAge;
		protected short		mAge;

		public Particle(Vector3f velocity, short maxAge)
		{
			reinit(velocity, maxAge);
		}

		public Color4f getColor()
		{
			return mColorTable.getColor(mAge);
		}

		public short getINum()
		{
			return 1;
		}

		protected Vector3f getLoc(short age)
		{
			tempVec.setX((mVelocity.getX() + mForce.getX()) * age);
			tempVec.setY((mVelocity.getY() + mForce.getY()) * age);
			tempVec.setZ((mVelocity.getZ() + mForce.getZ()) * age);
			return tempVec;
		}

		public short getVNum()
		{
			return 1;
		}

		public int getVPos(int index)
		{
			return index * 3 * getVNum();
		}

		public boolean isAlive()
		{
			return (mAge <= mMaxAge);
		}

		public void reinit(Vector3f velocity, short maxAge)
		{
			mLastPos = null;
			mVelocity = velocity;
			mMaxAge = maxAge;
			mAge = 0;
		}

		public boolean setLoc(int index)
		{
			if (mAge > mMaxAge)
			{
				return false;
			}
			Vector3f loc = getLoc(mAge);
			int vpos = index * 3;
			mVertexFbuf.position(vpos);
			mVertexFbuf.put(loc.getX()).put(loc.getY()).put(loc.getZ());
			mIndexSbuf.put((short) index);
			return true;
		}
	};

	protected class Particles
	{
		// List of particles
		protected Particle[]	mList;
		protected short			vNum;
		protected short			iNum;

		public void addParticles()
		{
			final int num = genCreateNum();
			if (num > 0)
			{
				int cnt = num;
				for (int i = 0; i < mList.length; i++)
				{
					if (mList[i] == null)
					{
						mList[i] = createParticle();
					}
					else if (!mList[i].isAlive())
					{
						reinit(mList[i]);
					}
					else
					{
						continue;
					}
					if (--cnt <= 0)
					{
						break;
					}
				}
			}
			if (ParticleSystem.DEBUG2)
			{
				Log.d(TAG, "Created " + num + " particles");
			}
		}

		// boolean checkAdd(int numVertex) {
		// boolean flag = true;
		// if (mVertexFbuf.position() + numVertex*3 >= mVertexFbuf.capacity()) {
		// Msg.err("Too many vertexes: " + mVertexFbuf.position() + "+3*" + numVertex + ">=" + mVertexFbuf.capacity());
		// flag = false;
		// }
		// if (mIndexSbuf.position() + numVertex >= mIndexSbuf.capacity()) {
		// Msg.err("Too many indexes: " + mIndexSbuf.position() + "+" + numVertex + ">=" + mIndexSbuf.capacity());
		// flag = false;
		// }
		// return flag;
		// }

		public Particle createParticle()
		{
			return new Particle(genVelocity(), genMaxAge());
		}

		public void init()
		{
			long maxLifeMs = mMaxAge * mAgeInterval;
			float maxNumCreatesPerLife = (float) maxLifeMs / (float) mCreateInterval;
			int size = (int) (mMaxCreate * maxNumCreatesPerLife);

			mList = new Particle[size];

			Particle sample = createParticle();
			vNum = sample.getVNum();
			iNum = sample.getINum();

			int bufSize = mParticles.mList.length * vNum;

			mVertexBuf = new FloatBuf();
			mVertexBuf.alloc(bufSize * 3);
			mVertexBuf.getBuf().limit(0);

			bufSize = mParticles.mList.length * iNum;
			mIndexBuf = new ShortBuf();
			mIndexBuf.alloc(bufSize);
			mIndexBuf.getBuf().limit(0);

			mNumAlive = 0;

			if (hasParticleColors())
			{
				mColorTable.setSize(mMaxAge);
			}
			else
			{
				mColorTable.clear();
			}
		}

		public void setLoc()
		{
			// Set ages of all particles
			mVertexFbuf = mVertexBuf.getBuf();
			mIndexSbuf = mIndexBuf.getBuf();

			mVertexFbuf.limit(mVertexFbuf.capacity());
			mIndexSbuf.limit(mIndexSbuf.capacity());

			mVertexFbuf.rewind();
			mIndexSbuf.rewind();

			mNumAlive = 0;

			for (int i = 0; i < mList.length; i++)
			{
				Particle part = mList[i];
				if (part == null)
				{
					break;
				}
				if (part.isAlive())
				{
					if (part.setLoc(i))
					{
						part.mAge++;
						mNumAlive++;
					}
				}
			}
			mVertexFbuf.limit(mVertexFbuf.position());
			mIndexSbuf.limit(mIndexSbuf.position());

			if (ParticleSystem.DEBUG2)
			{
				Log.d(TAG, "Vertexes=" + mVertexFbuf.limit() + ", indexes=" + mIndexSbuf.limit());

				mIndexSbuf.rewind();
				Vector3f max = new Vector3f();
				Vector3f min = new Vector3f();
				Vector3f v = new Vector3f();
				boolean first = true;

				while (mIndexSbuf.hasRemaining())
				{
					int index = mIndexSbuf.get();
					mVertexFbuf.position(index * 3);
					v.setX(mVertexFbuf.get());
					v.setY(mVertexFbuf.get());
					v.setZ(mVertexFbuf.get());
					if (first)
					{
						min.set(v);
						max.set(v);
						first = false;
					}
					else
					{
						if (v.getX() < min.getX())
						{
							min.setX(v.getX());
						}
						if (v.getY() < min.getY())
						{
							min.setY(v.getY());
						}
						if (v.getZ() < min.getZ())
						{
							min.setZ(v.getZ());
						}
						if (v.getX() > max.getX())
						{
							max.setX(v.getX());
						}
						if (v.getY() > max.getY())
						{
							max.setY(v.getY());
						}
						if (v.getZ() > max.getZ())
						{
							max.setZ(v.getZ());
						}
					}
				}
				Log.d(TAG, "Min=" + min.toString() + ", Max=" + max.toString());

				mIndexSbuf.rewind();
			}
		}
	}

	protected class Timing
	{
		protected long		mCurTime;
		protected long		mLastUpdate	= 0;
		protected long		mLastCreate	= 0;
		protected Timer		mTimer		= null;
		protected Boolean	mScheduled	= false;

		public Timing()
		{
		}

		synchronized void activity()
		{
			mParticleSystem.getView().requestRender();
			mScheduled = false;
		}

		public long getCurTime()
		{
			return mCurTime;
		}

		void onDrawStart()
		{
			mCurTime = System.currentTimeMillis();
		}

		boolean readyForCreate()
		{
			long diff = mCurTime - mLastCreate;
			if (diff >= mCreateInterval)
			{
				mLastCreate = mCurTime;
				return true;
			}
			return false;
		}

		boolean readyForUpdate()
		{
			long diff = mCurTime - mLastUpdate;
			if (diff >= mAgeInterval)
			{
				mLastUpdate = mCurTime;
				return true;
			}
			return false;
		}

		synchronized void cancel()
		{
			if (mTimer != null)
			{
				mTimer.cancel();
				mTimer = new Timer();
			}
			mScheduled = false;
			mLastCreate = 0;
			mLastUpdate = 0;
		}

		synchronized void schedule()
		{
			if (!mScheduled && mTimer != null)
			{
				long diffC = mCreateInterval - (mCurTime - mLastCreate);
				long diffU = mAgeInterval - (mCurTime - mLastUpdate);
				long diff;

				if (diffC < diffU)
				{
					diff = diffC;
				}
				else
				{
					diff = diffU;
				}
				if (diff <= 0)
				{
					mParticleSystem.getView().requestRender();
				}
				else
				{
					mTimer.schedule(new TimerTask()
					{
						@Override
						public void run()
						{
							activity();
						}
					}, diff);
					mScheduled = true;
				}
			}
		}

		synchronized void start()
		{
			cancel();
			mTimer = new Timer();
			schedule();
		}

		synchronized void stop()
		{
			cancel();
			mTimer = null;
		}
	};

	static final String			TAG				= "Emitter";

	static protected Vector3f	tempVec			= new Vector3f();

	// The min num of new particles to create per designated interval
	protected short				mMinCreate		= 25;
	// The max num of new particles to create per designated interval
	protected short				mMaxCreate		= 35;
	protected short				mDiffCreate		= 10;
	// How often to create
	protected short				mCreateInterval	= 30;
	// Shortest time a particle lives
	protected short				mMinAge			= 90;
	// Longest time a particle lives
	protected short				mMaxAge			= 110;
	protected short				mAgeDiff		= 20;
	// How often to age particles
	protected short				mAgeInterval	= 30;
	// Minimum velocity of particle
	protected float				mMinStr			= 0.05f;
	// Maximum velocity of particle
	protected float				mMaxStr			= 0.05f;
	protected float				mDiffStr		= 0;
	// Minimum direction vector
	protected Vector3f			mMinDir			= new Vector3f(-1, -1, -1);
	// Maximum direction vector
	protected Vector3f			mMaxDir			= new Vector3f(1, 1, 1);
	protected Vector3f			mDiffDir		= new Vector3f(2, 2, 2);
	// Control when each draw occurs
	protected Timing			mTiming			= new Timing();
	// Associated ParticleSystem
	protected ParticleSystem	mParticleSystem;
	// Colors
	protected Color4f			mStartColor		= Color4f.BLACK;
	protected Color4f			mEndColor		= null;
	protected ColorTable		mColorTable		= new ColorTable();
	// Random generator
	protected Random			mRandom;
	// Common force applied to all particles
	protected Vector3f			mForce			= new Vector3f();
	protected Particles			mParticles;
	protected int				mNumAlive		= 0;

	protected FloatBuf			mVertexBuf;
	protected FloatBuffer		mVertexFbuf;

	protected ShortBuf			mIndexBuf;
	protected ShortBuffer		mIndexSbuf;

	public Emitter()
	{
		mTiming = new Timing();
		mParticles = new Particles();
		mRandom = new Random();
	}

	public int genCreateNum()
	{
		return mMinCreate + mRandom.nextInt(mDiffCreate + 1);
	}

	protected short genMaxAge()
	{
		return (short) (mMinAge + mRandom.nextInt(mAgeDiff + 1));
	}

	protected Vector3f genVelocity()
	{
		Vector3f vec = new Vector3f(mMinDir.getX() + (mRandom.nextFloat() * mDiffDir.getX()), mMinDir.getY()
				+ (mRandom.nextFloat() * mDiffDir.getY()), mMinDir.getZ() + (mRandom.nextFloat() * mDiffDir.getZ()));
		vec.normalize();
		float strength = mMinStr + (mDiffStr * mRandom.nextFloat());
		return vec.multiply(strength);
	}

	public short getAgeInterval()
	{
		return mAgeInterval;
	}

	public FloatBuffer getColorBuf()
	{
		return null;
	}

	public short getMinCreate()
	{
		return mMinCreate;
	}

	public short getMaxCreate()
	{
		return mMaxCreate;
	}

	public short getCreateInterval()
	{
		return mCreateInterval;
	}

	public Color4f getEndColor()
	{
		return mEndColor;
	}

	public Vector3f getForce()
	{
		return mForce;
	}

	public Color4f getGeneralColor()
	{
		if (mStartColor == null && mEndColor == null)
		{
			return null;
		}
		if (mStartColor == null)
		{
			return mEndColor;
		}
		if (mEndColor == null)
		{
			return mStartColor;
		}
		if (mStartColor.equals(mEndColor))
		{
			return mStartColor;
		}
		return null;
	}

	public ShortBuffer getIndexBuf()
	{
		return mIndexBuf.getBuf();
	}

	public ShortBuffer getIndexBuf(int i)
	{
		return null;
	}

	public short getMinAge()
	{
		return mMinAge;
	}

	public short getMaxAge()
	{
		return mMaxAge;
	}

	public float getMaxDistance()
	{
		return mMaxStr * mMaxAge;
	}

	public FloatBuffer getNormalBuf()
	{
		return null;
	}

	public Color4f getParticleColor(int i)
	{
		return mParticles.mList[i].getColor();
	}

	public int getParticleCount()
	{
		return mNumAlive;
	}

	public Color4f getStartColor()
	{
		return mStartColor;
	}

	public float getMinStr()
	{
		return mMinStr;
	}

	public float getMaxStr()
	{
		return mMaxStr;
	}

	public Texture getTexture()
	{
		return null;
	}

	public FloatBuffer getTextureBuf()
	{
		return null;
	}

	public FloatBuffer getVertexBuf()
	{
		return mVertexBuf.getBuf();
	}

	public boolean hasParticleColors()
	{
		return mStartColor != null && mEndColor != null;
	}

	public void init()
	{
		mParticles.init();
	}

	public void reinit(Particle part)
	{
		part.reinit(genVelocity(), genMaxAge());
	}

	public void setAge(int ageInterval, int minAge, int maxAge)
	{
		mAgeInterval = (short) ageInterval;
		mMinAge = (short) minAge;
		mMaxAge = (short) maxAge;
		mAgeDiff = (short) (maxAge - minAge);
	}

	public void setCreate(int createInterval, int minCreate, int maxCreate)
	{
		mCreateInterval = (short) createInterval;
		mMinCreate = (short) minCreate;
		mMaxCreate = (short) maxCreate;
	}

	public void setEndColor(Color4f color)
	{
		mEndColor = color;
	}

	public void setForce(Vector3f force)
	{
		mForce = force;
	}

	public void setGeneralColor(Color4f color)
	{
		mStartColor = color;
		mEndColor = null;
	}

	public void setParticleSystem(ParticleSystem ps)
	{
		mParticleSystem = ps;
	}

	public void setRandom()
	{
		mRandom = new Random();
	}

	public void setRandomSeed(long seed)
	{
		mRandom = new Random(seed);
	}

	public void setStartColor(Color4f color)
	{
		mStartColor = color;
	}

	public void setStrength(float minStr, float maxStr)
	{
		mMinStr = minStr;
		mMaxStr = maxStr;
		mDiffStr = maxStr - minStr;
	}

	public void setDirection(Vector3f minDir, Vector3f maxDir)
	{
		mMinDir = minDir;
		mMaxDir = maxDir;
		mDiffDir = new Vector3f(maxDir).subtract(minDir);
	}
}
