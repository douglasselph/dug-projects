package com.tipsolutions.jacket.math;

import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;

public class Vector3f {
	
	static public final Vector3f UNIT_X = new Vector3f(1, 0, 0);
	static public final Vector3f UNIT_Y = new Vector3f(0, 1, 0);
	static public final Vector3f UNIT_Z = new Vector3f(0, 0, 1);
	static public final Vector3f ZERO = new Vector3f(0, 0, 0);
	
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
	
	public void apply(GL10 gl) {
		gl.glTranslatef(getX(), getY(), getZ());
	}
	
	public Vector3f cross(final Vector3f v) {
		float x = mX;
		float y = mY;
		mX = (y * v.mZ) - (mZ * v.mY);
		mY = (mZ * v.mX) - (x * v.mZ);
		mZ = (x * v.mY) - (y * v.mX);
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
	
	public Vector3f dup() {
		return new Vector3f(this);
	}
	
	public boolean equals(final Vector3f v) {
		return (mX == v.mX) && (mY == v.mY) && (mZ == v.mZ);
	}
	
	public float getX() { return mX; }
	public float getY() { return mY; }
	public float getZ() { return mZ; }
	
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
	
	public Vector3f set(final Vector3f v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
		return this;
	}
	
	public void setX(float x) { mX = x; }
	public void setY(float y) { mY = y; }
	public void setZ(float z) { mZ = z; }
	
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
	public void zero() { mX = 0; mY = 0; mZ = 0; }

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("(");
		sbuf.append(mX);
		sbuf.append(",");
		sbuf.append(mY);
		sbuf.append(",");
		sbuf.append(mZ);
		sbuf.append(")");
		return sbuf.toString();
	}
	
}
