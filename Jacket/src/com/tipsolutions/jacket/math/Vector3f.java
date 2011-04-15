package com.tipsolutions.jacket.math;

import android.util.FloatMath;

public class Vector3f {
	
	protected float mX = 0;
	protected float mY = 0;
	protected float mZ = 0;
	
	public Vector3f() {
	}
	
	public Vector3f(float x, float y, float z) {
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Vector3f(final Vector3f v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
	}
	
	public Vector3f add(float x, float y, float z) {
		mX += x;
		mY += y;
		mZ += z;
		return this;
	}
	
	public Vector3f add(final Vector3f v) {
		mX += v.mX;
		mY += v.mY;
		mZ += v.mZ;
		return this;
	}
	
	public Vector3f divide(float scalar) {
		mX /= scalar;
		mY /= scalar;
		mZ /= scalar;
		return this;
	}
	
	public Vector3f divide(float x, float y, float z) {
		mX /= x;
		mY /= y;
		mZ /= z;
		return this;
	}
	
	public Vector3f divide(final Vector3f v) {
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
	
	public Vector3f multiply(float scalar) {
		mX *= scalar;
		mY *= scalar;
		mZ *= scalar;
		return this;
	}
	
	public Vector3f multiply(float x, float y, float z) {
		mX *= x;
		mY *= y;
		mZ *= z;
		return this;
	}
	
	public Vector3f multiply(final Vector3f v) {
		mX *= v.mX;
		mY *= v.mY;
		mZ *= v.mZ;
		return this;
	}
	
	public Vector3f negate() {
		return multiply(-1f);
	}
	
	public Vector3f normalize() {
		final double lengthSq = lengthSquared();
		if (Math.abs(lengthSq) > MathUtils.EPSILON) {
			return multiply((float) MathUtils.inverseSqrt(lengthSq));
		}
		return this;
	}
	
	public Vector3f subtract(float x, float y, float z) {
		mX -= x;
		mY -= y;
		mZ -= z;
		return this;
	}
	
	public Vector3f subtract(final Vector3f v) {
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
