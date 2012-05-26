package com.tipsolutions.jacket.terrain;

import com.tipsolutions.jacket.math.Vector3f;

/**
 * This is what is computed at a given point for a generator.
 */
public class Info {
	float mHeight;
	Vector3f mNormal;
	
	public Info(float height, Vector3f normal) {
		mHeight = height;
		mNormal = normal;
	}
	
	public Info(float height) {
		mHeight = height;
	}
	
	public Info() {
	}
	
	public float getHeight() {
		return mHeight;
	}
	
	public Vector3f getNormal() {
		return mNormal;
	}
	
	public void setHeight(float height) {
		mHeight = height;
	}
	
	public void setNormal(Vector3f normal) {
		mNormal = normal;
	}
}
