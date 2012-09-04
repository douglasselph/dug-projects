package com.dugsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.util.Log;

import com.dugsolutions.jacket.image.TextureManager.Texture;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.shape.BufferUtils.FloatBuf;

public class EmitterTex extends Emitter {

	class ParticlesTex extends Particles {

		@Override
		public Particle createParticle() {
			return new ParticleTex(genVelocity(), genMaxAge());
		}

		@Override
		public void init() {
			super.init();
			
			int bufSize = mParticles.mList.length * vNum;
			mTextureBuf = new FloatBuf();
			mTextureBuf.alloc(bufSize*2);
			FloatBuffer fbuf = mTextureBuf.getBuf();
			
			int count = mTextureBuf.capacity()/8;
			for (int i = 0; i < count; i++) {
				fbuf.put(0).put(0);
				fbuf.put(0).put(1);
				fbuf.put(1).put(0);
				fbuf.put(1).put(1);
			}
			fbuf.limit(0);
			
			mNormalBuf = new FloatBuf();
			mNormalBuf.alloc(mVertexBuf.capacity());
			fbuf = mNormalBuf.getBuf();
			count = mNormalBuf.capacity()/3;
			for (int i = 0; i < count; i++) {
    			fbuf.put(0).put(0).put(1);
			}
			fbuf.limit(0);
			
			if (!USE_STRIPS && hasParticleColors()) {
				bufSize = mParticles.mList.length * vNum;
				mColorBuf = new FloatBuf();
				mColorBuf.alloc(bufSize*4);
				mColorBuf.getBuf().limit(0);
			} else {
				mColorBuf = null;
			}
			mDeltaSize = mEndSize - mStartSize;
		}
		
		@Override
		public void setLoc() {
			if (mColorBuf != null) {
				mColorFbuf = mColorBuf.getBuf();
				mColorFbuf.limit(mColorFbuf.capacity());
				mColorFbuf.rewind();
			} else {
				mColorFbuf = null;
			}
			super.setLoc();
			
			int vlimit = mVertexBuf.getBuf().limit();
			mNormalBuf.getBuf().limit(vlimit);
			int tlimit = (vlimit/3)*2;
			mTextureBuf.getBuf().limit(tlimit);
			
			if (mColorFbuf != null) {
				mColorFbuf.limit(mColorFbuf.position());
			}
		}
	}

	class ParticleTex extends Particle {
		
		public ParticleTex(Vector3f velocity, short maxAge) {
			super(velocity, maxAge);
		}
		
		public int getCPos(int index) {
			return index*4*getVNum();
		}
		
		@Override
		public short getINum() {
			return (short)(USE_STRIPS ? 4 : 6);
		}
		
		public int getTPos(int index) {
			return index*2*getVNum();
		}
		
		@Override
		public short getVNum() {
			return 4;
		}

		@Override
		public boolean setLoc(int index) {
			if (mAge > mMaxAge) {
				return false;
			}
			Vector3f loc = getLoc(mAge);
			float x = loc.getX();
			float y = loc.getY();
			float z = loc.getZ();
		
			float halfSize = (mStartSize + mDeltaSize/mMaxAge * mAge)/2;
			float x1 = x - halfSize;
			float x2 = x + halfSize;
			float y1 = y - halfSize;
			float y2 = y + halfSize;
			if (ParticleSystem.DEBUG2) {
				Log.d("DEBUG", "" + index + ":x1=" + x1 + ", x2=" + x2 + ", y1=" + y1 + ", y2=" + y2);
			}
			int vpos = index*3*4;
			mVertexFbuf.position(vpos);
			mVertexFbuf.put(x1).put(y1).put(z);
			mVertexFbuf.put(x1).put(y2).put(z);
			mVertexFbuf.put(x2).put(y1).put(z);
			mVertexFbuf.put(x2).put(y2).put(z);

			short [] vA;
			short ipos = (short) (index*4);

			if (USE_STRIPS) {
				vA = new short[4];
				vA[0] = ipos;
				vA[1] = (short) (ipos+1);
				vA[2] = (short) (ipos+2);
				vA[3] = (short) (ipos+3);
			} else {
				vA = new short[6];

				vA[0] = ipos;
				vA[1] = (short) (ipos+1);
				vA[2] = (short) (ipos+2);
				vA[3] = (short) (ipos+3);
				vA[4] = (short) vA[2];
				vA[5] = (short) vA[1];

				if (mColorFbuf != null) {
					Color4f color = mColorTable.getColor(mAge);
					mColorFbuf.position(index*4*4);
					for (int i = 0; i < 4; i++) {
						mColorFbuf.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
					}
				}
			}
			mIndexSbuf.put(vA);
			return true;
		}
		
	};
	
	// Note: tests reveal that the strip case is almost twice as slow!
	//  Average Strip Time = 92ms
	//  Average Non-Strip Time = 57ms
	public static final Boolean USE_STRIPS = false;;
	
	protected Texture mTexture;
	protected float mStartSize = 0.1f;
	protected float mEndSize = 0.1f;
	float mDeltaSize;
	
	protected FloatBuf mTextureBuf;
	protected FloatBuf mNormalBuf;
	protected FloatBuf mColorBuf = null;
	protected FloatBuffer mColorFbuf = null;

	public EmitterTex(Texture tex) {
		super();
		mParticles = new ParticlesTex();
		mTexture = tex;
	}
	
	@Override
	public FloatBuffer getColorBuf() { 
		if (mColorBuf == null){
			return null;
		}
		return mColorBuf.getBuf(); 
	}
	
	@Override
	public ShortBuffer getIndexBuf(int i) {
		ShortBuffer ibuf = mIndexBuf.getBuf();
		ibuf.position(i*mParticles.iNum);
		return ibuf;
	}
	
	@Override
	public FloatBuffer getNormalBuf() { return mNormalBuf.getBuf(); }

	public float getStartSize() { return mStartSize; }
	public float getEndSize(){ return mEndSize; }
	
	@Override
	public Texture getTexture() { return mTexture; }

	@Override
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }

	public void setSize(float startSize, float endSize) { 
		mStartSize = startSize;
		mEndSize = endSize;
	}
	public void setTexture(Texture t) { mTexture = t; }
}
