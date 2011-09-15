package com.tipsolutions.jacket.math;

public class Vector4f extends Vector3f {

	public Vector4f() {
		mData = new float[4];
	}

	public Vector4f(float x, float y, float z) {
		mData = new float[4];
		mData[0] = x;
		mData[1] = y;
		mData[2] = z;
		mData[3] = 1;
	}

	public Vector4f(Vector3 v) {
		mData = new float[4];
		mData[0] = v.getXf();
		mData[1] = v.getYf();
		mData[2] = v.getZf();
		mData[3] = 1;
	}

	public Vector4f(double x, double y, double z) {
		mData = new float[4];
		mData[0] = (float) x;
		mData[1] = (float) y;
		mData[2] = (float) z;
		mData[3] = 1;
	}
	
	public Vector4f(Vector3f v) {
		mData = new float[4];
		int i;
		for (i = 0; i < mData.length && i < v.mData.length; i++) {
			mData[i] = v.mData[i];
		}
		if (i < 4) {
			mData[3] = 1;
		}
	}
	
	public Vector4f(float x, float y, float z, float w) {
		mData[0] = x;
		mData[1] = y;
		mData[2] = z;
		mData[3] = w;
	}
	
	public Vector4f(float [] v) {
		int i;
		for (i = 0; i < mData.length && i < v.length; i++) {
			mData[i] = v[i];
		}
		if (i < 4) {
			mData[3] = 1;
		}
	}
	public float getW() { return mData[3]; }
}
