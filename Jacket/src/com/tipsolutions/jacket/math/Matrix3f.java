package com.tipsolutions.jacket.math;

import android.util.FloatMath;

public class Matrix3f {
	
    public final static Matrix3f IDENTITY = new Matrix3f(1f, 0, 0, 0, 1, 0, 0, 0, 1f);
    
    protected final float[][] mData = new float[3][3];
    
    public Matrix3f() {
        this(IDENTITY);
    }
    
    public Matrix3f(
    		final float m00, final float m01, final float m02, 
    		final float m10, final float m11, final float m12, 
    		final float m20, final float m21, final float m22) {
        mData[0][0] = m00;
        mData[0][1] = m01;
        mData[0][2] = m02;
        mData[1][0] = m10;
        mData[1][1] = m11;
        mData[1][2] = m12;
        mData[2][0] = m20;
        mData[2][1] = m21;
        mData[2][2] = m22;
    }

    public Matrix3f(final Matrix3f source) {
        set(source);
    }
    
    public Vector3f apply(final Vector3f vec) {
    	return apply(vec, null);
    }
    /**
     * Multiplies the given vector by this matrix (M * v). 
     * If supplied, the result is stored into the supplied "store" vector.
     * 
     * @param vec
     *            the vector to multiply this matrix by.
     * @param store
     *            a vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    public Vector3f apply(final Vector3f vec, Vector3f store) {
        Vector3f result = store;
        if (result == null) {
            result = new Vector3f();
        }
        final float x = vec.getX();
        final float y = vec.getY();
        final float z = vec.getZ();

        result.setX(mData[0][0] * x + mData[0][1] * y + mData[0][2] * z);
        result.setY(mData[1][0] * x + mData[1][1] * y + mData[1][2] * z);
        result.setZ(mData[2][0] * x + mData[2][1] * y + mData[2][2] * z);
        return result;
    }

    
    /**
     * Sets this matrix to the rotation indicated by the given angle and a unit-length axis of rotation.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix3f fromAngleNormalAxis(final float angle, final Vector3f axis) {
        final float fCos = FloatMath.cos(angle);
        final float fSin = FloatMath.sin(angle);
        final float fOneMinusCos = 1f - fCos;
        final float fX2 = axis.getX() * axis.getX();
        final float fY2 = axis.getY() * axis.getY();
        final float fZ2 = axis.getZ() * axis.getZ();
        final float fXYM = axis.getX() * axis.getY() * fOneMinusCos;
        final float fXZM = axis.getX() * axis.getZ() * fOneMinusCos;
        final float fYZM = axis.getY() * axis.getZ() * fOneMinusCos;
        final float fXSin = axis.getX() * fSin;
        final float fYSin = axis.getY() * fSin;
        final float fZSin = axis.getZ() * fSin;

        mData[0][0] = fX2 * fOneMinusCos + fCos;
        mData[0][1] = fXYM - fZSin;
        mData[0][2] = fXZM + fYSin;
        mData[1][0] = fXYM + fZSin;
        mData[1][1] = fY2 * fOneMinusCos + fCos;
        mData[1][2] = fYZM - fXSin;
        mData[2][0] = fXZM - fYSin;
        mData[2][1] = fYZM + fXSin;
        mData[2][2] = fZ2 * fOneMinusCos + fCos;

        return this;
    }

    public float getValue(final int row, final int column) {
        return mData[row][column];
    }
    
    public Matrix3f set(final Matrix3f source) {
        mData[0][0] = source.getValue(0, 0);
        mData[1][0] = source.getValue(1, 0);
        mData[2][0] = source.getValue(2, 0);

        mData[0][1] = source.getValue(0, 1);
        mData[1][1] = source.getValue(1, 1);
        mData[2][1] = source.getValue(2, 1);

        mData[0][2] = source.getValue(0, 2);
        mData[1][2] = source.getValue(1, 2);
        mData[2][2] = source.getValue(2, 2);

        return this;
    }
}
