package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;

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
				mColorBuf = new FloatBuf();
				mColorBuf.alloc(vNum*4);
				mColorBuf.getBuf().limit(0);
			} else {
				mColorBuf = null;
			}
		}
		
		@Override
		public void setLoc() {
			if (mColorBuf != null) {
				mColorFbuf = mColorBuf.getBuf();
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

		public ParticleTex(Vector3f velocity, int maxAge) {
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
			int age = getAge();
			
			if (age > mMaxAge) {
				mMaxAge = 0;
				return false;
			}
			Vector3f loc = getLoc(age);
			float x = loc.getX();
			float y = loc.getY();
			float z = loc.getZ();

			float x1 = x - mParticleHalfSize;
			float x2 = x + mParticleHalfSize;
			float y1 = y - mParticleHalfSize;
			float y2 = y + mParticleHalfSize;

			int vpos = (short) getVPos(index);
			mVertexFbuf.position(vpos);
			mVertexFbuf.put(x1).put(y1).put(z);
			mVertexFbuf.put(x1).put(y2).put(z);
			mVertexFbuf.put(x2).put(y1).put(z);
			mVertexFbuf.put(x2).put(y2).put(z);

			short [] vA;

			if (USE_STRIPS) {
				vA = new short[4];
				vA[0] = (short) (vpos);
				vA[1] = (short) (vpos+1);
				vA[2] = (short) (vpos+2);
				vA[3] = (short) (vpos+3);
			} else {
				vA = new short[6];

				vA[0] = (short) (vpos);
				vA[1] = (short) (vpos+1);
				vA[2] = (short) (vpos+2);
				vA[3] = (short) (vpos+3);
				vA[4] = (short) vA[2];
				vA[5] = (short) vA[1];

				if (mColorFbuf != null) {
					Color4f color = mColorTable.getColor(age);
					mColorFbuf.position(getCPos(index));
					for (int i = 0; i < 4; i++) {
						mColorFbuf.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
					}
				}
			}
			for (short v : vA) {
				mIndexSbuf.put(v);
			}
			return true;
		}
		
	};
	
	// Note: tests reveal that the strip case is almost twice as slow!
	//  Average Strip Time = 92ms
	//  Average Non-Strip Time = 57ms
	public static final Boolean USE_STRIPS = false;;
	
	protected Texture mTexture;
	protected float mParticleHalfSize = 0.1f;
	
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

	public float getParticleSize() { return mParticleHalfSize*2; }
	
	@Override
	public Texture getTexture() { return mTexture; }

	@Override
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }

	public void setParticleSize(float v) { mParticleHalfSize = v/2; }
	public void setTexture(Texture t) { mTexture = t; }
}
