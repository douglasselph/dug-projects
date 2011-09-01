package com.tipsolutions.jacket.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.util.Log;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Constants;
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
    			mVertexFbuf.position(getVbufPos(index));
    			mVertexFbuf.put(x-mParticleHalfSize).put(y+mParticleHalfSize).put(z);
    			mVertexFbuf.put(x-mParticleHalfSize).put(y-mParticleHalfSize).put(z);
    			mVertexFbuf.put(x+mParticleHalfSize).put(y-mParticleHalfSize).put(z);
    			mVertexFbuf.put(x+mParticleHalfSize).put(y+mParticleHalfSize).put(z);
    			
    			mTextureFbuf.position(getTbufPos(index));
    			mTextureFbuf.put(0).put(1);
    			mTextureFbuf.put(0).put(0);
    			mTextureFbuf.put(1).put(0);
    			mTextureFbuf.put(1).put(1);
			}
		}
		
		int getTbufPos(int index) {
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
			mTextureBuf.getBuf().limit(0);
		}

		@Override
		public void setLoc() {
			mTextureFbuf = mTextureBuf.getBuf();
			mTextureFbuf.limit(mTextureFbuf.capacity());
			mTextureFbuf.rewind();
			super.setLoc();
			mTextureFbuf.limit(mTextureFbuf.position());
		}
		
		@Override
		public Particle createParticle() {
			return new ParticleTex(genVelocity(), genMaxAge());
		}

		@Override
		boolean checkAdd(int numVertex) {
			boolean flag = super.checkAdd(numVertex);
			
			if (mTextureFbuf.position() + numVertex*2 >= mTextureFbuf.capacity()) {
				Log.e(Constants.TAG, "Too many texcoords: " + mTextureFbuf.position() + "+2*" + numVertex + ">=" + mTextureFbuf.capacity());
				flag = false;
			}
			return flag;
		}
	};
	
	protected Texture mTexture;
	protected float mParticleHalfSize;
	protected FloatBuf mTextureBuf;
	protected FloatBuffer mTextureFbuf;

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
	public ShortBuffer getIndexBuf(int i) {
		ShortBuffer ibuf = mIndexBuf.getBuf();
		ibuf.position(i*4);
		return ibuf;
	}
	
}
