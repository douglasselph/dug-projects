package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.BufferUtils.FloatBuf;

public class EmitterTex extends Emitter {

	class ParticleTex extends Particle {

		public ParticleTex(Vector3f velocity, boolean doColor) {
			super(velocity, doColor);
		}

		public ParticleTex(Vector3f velocity, int maxAge) {
			super(velocity, maxAge);
		}
		
		@Override
		public void setLoc(int index) {
			int age = getAge();
			
			if (age > mMaxAge) {
				mMaxAge = 0;
			} else {
    			Vector3f loc = getLoc(age);
    			float x = loc.getX();
    			float y = loc.getY();
    			float z = loc.getZ();
    			
    			float x1 = x - mParticleHalfSize;
    			float x2 = x + mParticleHalfSize;
    			float y1 = y - mParticleHalfSize;
    			float y2 = y + mParticleHalfSize;
    			
    			mVertexFbuf.position(getVPos(index));
    			mVertexFbuf.put(x1).put(y1).put(z);
    			mVertexFbuf.put(x1).put(y2).put(z);
    			mVertexFbuf.put(x2).put(y1).put(z);
    			mVertexFbuf.put(x2).put(y2).put(z);
			}
		}
		
		public int getTPos(int index) {
			return index*2*getNumVertex();
		}

		@Override
		public int getNumVertex() {
			return 4;
		}
		
	};
	
	class ParticlesTex extends Particles {

		@Override
		public void init() {
			super.init();
			
			mTextureBuf = new FloatBuf();
			mTextureBuf.alloc(mIndexBuf.capacity()*2);
			FloatBuffer fbuf = mTextureBuf.getBuf();
			
			for (int i = 0; i < mTextureBuf.capacity()/8; i++) {
				fbuf.put(0).put(0);
				fbuf.put(0).put(1);
				fbuf.put(1).put(0);
				fbuf.put(1).put(1);
			}
			fbuf.limit(0);
			
			mNormalBuf = new FloatBuf();
			mNormalBuf.alloc(mVertexBuf.capacity());
			fbuf = mNormalBuf.getBuf();
			for (int i = 0; i < mNormalBuf.capacity()/3; i++) {
    			fbuf.put(0).put(0).put(1);
			}
			fbuf.limit(0);
		}

		@Override
		public void setLoc() {
			super.setLoc();
			
			int vlimit = mVertexBuf.getBuf().limit();
			mNormalBuf.getBuf().limit(vlimit);
			int tlimit = (vlimit/3)*2;
			mTextureBuf.getBuf().limit(tlimit);
		}
		
		@Override
		public Particle createParticle() {
			return new ParticleTex(genVelocity(), genMaxAge());
		}
	};
	
	protected Texture mTexture;
	protected float mParticleHalfSize;
	
	protected FloatBuf mTextureBuf;
	protected FloatBuf mNormalBuf;

	public EmitterTex(Texture tex, float particleSize,
			int frameInterval, 
			int createPerFrame, int createVariance, 
			int maxAgeInitial, int maxAgeVariance,
			float strength, float strengthVariance) {
		super(frameInterval, 
				createPerFrame, createVariance, maxAgeInitial,
				maxAgeVariance, strength, strengthVariance);
		mParticles = new ParticlesTex();
		mParticleHalfSize = particleSize/2;
		mTexture = tex;
	}

	@Override
	public Texture getTexture() {
		return mTexture;
	}

	@Override
	public FloatBuffer getTextureBuf() {
		return mTextureBuf.getBuf();
	}

	@Override
	public FloatBuffer getNormalBuf() {
		return mNormalBuf.getBuf();
	}

	@Override
	public ShortBuffer getIndexBuf(int i) {
		ShortBuffer ibuf = mIndexBuf.getBuf();
		ibuf.position(i*4);
		return ibuf;
	}
	
}
