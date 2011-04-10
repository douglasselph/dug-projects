package com.tipsolutions.jacket.math;

import android.util.FloatMath;

public class Vertex3 {
	
	protected float mX = 0;
	protected float mY = 0;
	protected float mZ = 0;
	
	public Vertex3() {
	}
	
	public Vertex3(float x, float y, float z) {
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Vertex3(final Vertex3 v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
	}
	
	public Vertex3 add(float x, float y, float z) {
		mX += x;
		mY += y;
		mZ += z;
		return this;
	}
	
	public Vertex3 add(final Vertex3 v) {
		mX += v.mX;
		mY += v.mY;
		mZ += v.mZ;
		return this;
	}
	
	public Vertex3 divide(float scalar) {
		mX /= scalar;
		mY /= scalar;
		mZ /= scalar;
		return this;
	}
	
	public Vertex3 divide(float x, float y, float z) {
		mX /= x;
		mY /= y;
		mZ /= z;
		return this;
	}
	
	public Vertex3 divide(final Vertex3 v) {
		mX /= v.mX;
		mY /= v.mY;
		mZ /= v.mZ;
		return this;
	}
	
	public float length() {
		return FloatMath.sqrt(lengthSquared());
	}

	public float lengthSquared() {
		return getX() * getX() + getY() * getY() + getZ() * getZ();
	}
	
	public Vertex3 multiply(float scalar) {
		mX *= scalar;
		mY *= scalar;
		mZ *= scalar;
		return this;
	}
	
	public Vertex3 multiply(float x, float y, float z) {
		mX *= x;
		mY *= y;
		mZ *= z;
		return this;
	}
	
	public Vertex3 multiply(final Vertex3 v) {
		mX *= v.mX;
		mY *= v.mY;
		mZ *= v.mZ;
		return this;
	}
	
	public Vertex3 negate() {
		return multiply(-1f);
	}
	
	public Vertex3 normalize() {
		final double lengthSq = lengthSquared();
		if (Math.abs(lengthSq) > MathUtils.EPSILON) {
			return multiply((float) MathUtils.inverseSqrt(lengthSq));
		}
		return this;
	}
	
	public Vertex3 subtract(float x, float y, float z) {
		mX -= x;
		mY -= y;
		mZ -= z;
		return this;
	}
	
	public Vertex3 subtract(final Vertex3 v) {
		mX -= v.mX;
		mY -= v.mY;
		mZ -= v.mZ;
		return this;
	}
	
	public float getX() { return mX; }
	
	public float getY() { return mY; }
	public float getZ() { return mZ; }
	public void setX(float x) { mX = x; }
	
	public void setY(float y) { mY = y; }
	public void setZ(float z) { mZ = z; }
	public void zero() { mX = 0; mY = 0; mZ = 0; }
}
