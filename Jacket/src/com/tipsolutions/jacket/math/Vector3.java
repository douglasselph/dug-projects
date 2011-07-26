package com.tipsolutions.jacket.math;



public class Vector3 {
	
	static public final Vector3 UNIT_X = new Vector3(1, 0, 0);
	static public final Vector3 UNIT_Y = new Vector3(0, 1, 0);
	static public final Vector3 UNIT_Z = new Vector3(0, 0, 1);
	static public final Vector3 ZERO = new Vector3(0, 0, 0);
	
	protected double mX = 0;
	protected double mY = 0;
	protected double mZ = 0;
	
	public Vector3() {
	}
	
	public Vector3(double x, double y, double z) {
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Vector3(final Vector3 v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
	}
	
	public Vector3(final Vector3f v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
	}
	
	public Vector3 add(double x, double y, double z) {
		mX += x;
		mY += y;
		mZ += z;
		return this;
	}
	
	public Vector3 add(final Vector3 v) {
		mX += v.mX;
		mY += v.mY;
		mZ += v.mZ;
		return this;
	}
	
	public Vector3 cross(final Vector3 v) {
		double x = mX;
		double y = mY;
		mX = (y * v.mZ) - (mZ * v.mY);
		mY = (mZ * v.mX) - (x * v.mZ);
		mZ = (x * v.mY) - (y * v.mX);
		return this;
	}
	
	public Vector3 divide(double scalar) {
		mX /= scalar;
		mY /= scalar;
		mZ /= scalar;
		return this;
	}

	public Vector3 divide(double x, double y, double z) {
		mX /= x;
		mY /= y;
		mZ /= z;
		return this;
	}
	
	public Vector3 divide(final Vector3 v) {
		mX /= v.mX;
		mY /= v.mY;
		mZ /= v.mZ;
		return this;
	}
	
	public Vector3 dup() {
		return new Vector3(this);
	}
	
	public boolean equals(final Vector3 v) {
		return (mX == v.mX) && (mY == v.mY) && (mZ == v.mZ);
	}
	
	public double getX() { return mX; }
	public double getY() { return mY; }
	public double getZ() { return mZ; }
	
	public float getXf() { return (float)mX; }
	public float getYf() { return (float)mY; }
	public float getZf() { return (float)mZ; }
	
	public double length() {
		return Math.sqrt(lengthSquared());
	}
	
	public double lengthSquared() {
		return getX() * getX() + getY() * getY() + getZ() * getZ();
	}
	
	public Vector3 multiply(double scalar) {
		mX *= scalar;
		mY *= scalar;
		mZ *= scalar;
		return this;
	}
	
   /**
     * Multiplies the values of this vector by the given scalar value and returns the result in store.
     * 
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scalar, this.y * scalar, this.z * scalar)
     */
    public Vector3 multiply(double scalar, Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        return result.set(getX() * scalar, getY() * scalar, getZ() * scalar);
    }
	
	public Vector3 multiply(double x, double y, double z) {
		mX *= x;
		mY *= y;
		mZ *= z;
		return this;
	}
	
	public Vector3 multiply(final Vector3 v) {
		mX *= v.mX;
		mY *= v.mY;
		mZ *= v.mZ;
		return this;
	}
	
	public Vector3 negate() {
		return multiply(-1f);
	}
	
	public Vector3 normalize() {
		final double lengthSq = lengthSquared();
		if (Math.abs(lengthSq) > MathUtils.EPSILON) {
			return multiply(MathUtils.inverseSqrt(lengthSq));
		}
		return this;
	}
	
	public Vector3 set(final Vector3 v) {
		mX = v.mX;
		mY = v.mY;
		mZ = v.mZ;
		return this;
	}
	
	public Vector3 set(double x, double y, double z) { 
		mX = x; 
		mY = y; 
		mZ = z; 
		return this;
	}
	
	public void setX(double x) { mX = x; }
	public void setY(double y) { mY = y; }
	public void setZ(double z) { mZ = z; }
	
	public Vector3 subtract(double x, double y, double z) {
		mX -= x;
		mY -= y;
		mZ -= z;
		return this;
	}
	public Vector3 subtract(final Vector3 v) {
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
