package com.dugsolutions.jacket.math;

import android.util.FloatMath;

// Note: there is also android.graphics.Matrix and android.opengl.Matrix.
// This class is used by SliceData so that it does not depend on android
// libraries.
public class Matrix3f {

	public static final Matrix3f IDENTITY = new Matrix3f(1, 0, 0, 
														 0, 1, 0,
														 0, 0, 1);
	protected float [] mValues = new float[9];
	
	public Matrix3f() {
		this(IDENTITY);
	}

	public Matrix3f(Matrix3f src) {
		set(src);
	}
	
	public Matrix3f(
			final float m00, final float m01, final float m02, 
			final float m10, final float m11, final float m12, 
			final float m20, final float m21, final float m22) {
		set(m00, m01, m02,
			m10, m11, m12,
			m20, m21, m22);
	}
	
	public static int pos(int row, int col) {
		return row+col*3;
	}
	
	public void setRotate(float angleX, float angleY, float angleZ) {
		float Cx       = FloatMath.cos(angleX);
		float Sx       = FloatMath.sin(angleX);
		float Cy       = FloatMath.cos(angleY);
		float Sy       = FloatMath.sin(angleY);
		float Cz       = FloatMath.cos(angleZ);
		float Sz       = FloatMath.sin(angleZ);

		float CxSy      = Cx * Sy;
		float SxSy      = Sx * Sy;
		
		float [] values = new float[9];

		values[pos(0,0)] = Cy * Cz;  
		values[pos(0,1)] = Cy * -Sz; 
		values[pos(0,2)] = Sy;		  
		values[pos(1,0)] = SxSy * Cz + Cx * Sz;  
		values[pos(1,1)] = -SxSy * Sz + Cx * Cz; 
		values[pos(1,2)] = -Sx * Cy;
		values[pos(2,0)] = -CxSy * Cz + Sx * Sz;
		values[pos(2,1)] = CxSy * Sz + Sx * Cz;
		values[pos(2,2)] = Cx * Cy;
		
		mValues = values;
	}
//  
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
        float x = vec.getX();
        float y = vec.getY();
        float z = vec.getZ();
        
        final float [] values = mValues;

        result.setX(values[pos(0,0)] * x + values[pos(0,1)] * y + values[pos(0,2)] * z);
        result.setY(values[pos(1,0)] * x + values[pos(1,1)] * y + values[pos(1,2)] * z);
        result.setZ(values[pos(2,0)] * x + values[pos(2,1)] * y + values[pos(2,2)] * z);
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
        
        float [] values = new float[9];

        values[pos(0,0)] = fX2 * fOneMinusCos + fCos;
        values[pos(0,1)] = fXYM - fZSin;
        values[pos(0,2)] = fXZM + fYSin;
        values[pos(1,0)] = fXYM + fZSin;
        values[pos(1,1)] = fY2 * fOneMinusCos + fCos;
        values[pos(1,2)] = fYZM - fXSin;
        values[pos(2,0)] = fXZM - fYSin;
        values[pos(2,1)] = fYZM + fXSin;
        values[pos(2,2)] = fZ2 * fOneMinusCos + fCos;
        
        mValues = values;

        return this;
    }
    
    
// 
    public Vector3f getColumn(final int index) {
        if (index < 0 || index > 2) {
            throw new IllegalArgumentException("Illegal column index: " + index);
        }
        final float [] values = mValues;
        Vector3f result = new Vector3f();
        result.setX(values[pos(0,index)]);
        result.setY(values[pos(1,index)]);
        result.setZ(values[pos(2,index)]);
        return result;
    }
    
    public Vector3f getColumn(final int index, Vector3f store) {
        if (index < 0 || index > 2) {
            throw new IllegalArgumentException("Illegal column index: " + index);
        }
        final float [] values = mValues;
        store.setX(values[pos(0,index)]);
        store.setY(values[pos(1,index)]);
        store.setZ(values[pos(2,index)]);
        return store;
    }
    
    public Quaternion getQuaternion(){
    	return new Quaternion().fromRotationMatrix(this);
    }
    
    public float getValue(int row, int col) {
    	return mValues[pos(row,col)];
    }
    
    public float [] getValues() {
    	return mValues;
    }
//    
////    public Rotate getRotate() {
////    	/*
////        	 |  0  1  2  3 |
////        M =  |  4  5  6  7 |
////             |  8  9 10 11 |
////             | 12 13 14 15 |
////        */
////    	double angle_x, angle_y, angle_z;
////    	double C;
////    	double tr_x, tr_y;
////    	
////    	angle_y = -Math.asin(getValue(0,2)/*mat[2]*/);/* Calculate Y-axis angle */
////    	C       =  Math.cos( angle_y );
////
////    	if (Math.abs( C ) > 0.005)             /* Gimball lock? */
////    	{
////    		tr_x      =  getValue(2,2) /*mat[10]*// C; /* No, so get X-axis angle */
////    		tr_y      = -getValue(1,2) /*mat[6]*// C;
////
////    		angle_x  = Math.atan2( tr_y, tr_x );
////
////    		tr_x      =  getValue(0,0)/*mat[0]*/ / C; /* Get Z-axis angle */
////    		tr_y      = -getValue(0,1)/*mat[1]*/ / C;
////
////    		angle_z  = Math.atan2( tr_y, tr_x );
////    	} else { /* Gimball lock has occurred */
////    		angle_x  = 0;                      /* Set X-axis angle to zero */
////
////    		tr_x      = getValue(1,1)/*mat[5]*/; /* And calculate Z-axis angle */
////    		tr_y      = getValue(1,0)/*mat[4]*/;
////
////    		angle_z  = Math.atan2( tr_y, tr_x );
////    	}
////    	angle_x = MathUtils.clamp( angle_x );
////    	angle_y = MathUtils.clamp( angle_y );
////    	angle_z = MathUtils.clamp( angle_z );
////    	
////    	return new Rotate((float) angle_x, (float) angle_y, (float) angle_z);
////    }
//
//  
    public Matrix3f mult(final Matrix3f matrix) {
    	float data00 = getValue(0,0);
    	float data01 = getValue(0,1);
    	float data02 = getValue(0,2);
    	float data10 = getValue(1,0);
    	float data11 = getValue(1,1);
    	float data12 = getValue(1,2);
    	float data20 = getValue(2,0);
    	float data21 = getValue(2,1);
    	float data22 = getValue(2,2);
    	
    	float m00 = matrix.getValue(0,0);
    	float m01 = matrix.getValue(0,1);
    	float m02 = matrix.getValue(0,2);
    	float m10 = matrix.getValue(1,0);
    	float m11 = matrix.getValue(1,1);
    	float m12 = matrix.getValue(1,2);
    	float m20 = matrix.getValue(2,0);
    	float m21 = matrix.getValue(2,1);
    	float m22 = matrix.getValue(2,2);
    	
        double temp00 = data00 * m00 + data01 * m10 + data02 * m20;
        double temp01 = data00 * m01 + data01 * m11 + data02 * m21;
        double temp02 = data00 * m02 + data01 * m12 + data02 * m22;

        double temp10 = data10 * m00 + data11 * m10 + data12 * m20;
        double temp11 = data10 * m01 + data11 * m11 + data12 * m21;
        double temp12 = data10 * m02 + data11 * m12 + data12 * m22;

        double temp20 = data20 * m00 + data21 * m10 + data22 * m20;
        double temp21 = data20 * m01 + data21 * m11 + data22 * m21;
        double temp22 = data20 * m02 + data21 * m12 + data22 * m22;

        set(temp00, temp01, temp02, 
        	temp10, temp11, temp12, 
        	temp20, temp21, temp22);

        return this;
    }
    
    public Matrix3f set(double m00, double m01, double m02,
    					double m10, double m11, double m12,
    					double m20, double m21, double m22) {
    	return set((float)m00, (float) m01, (float) m02,
    			   (float)m10, (float) m11, (float) m12,
    			   (float)m20, (float) m21, (float) m22);
    }
    
    public Matrix3f set(float m00, float m01, float m02,
    					float m10, float m11, float m12,
    					float m20, float m21, float m22) {
        setValue(0,0,m00);
        setValue(1,0,m10);
        setValue(2,0,m20);

        setValue(0,1,m01);
        setValue(1,1,m11);
        setValue(2,1,m21);

        setValue(0,2,m02);
        setValue(1,2,m12);
        setValue(2,2,m22);
        return this;
    }
    
    public Matrix3f set(final Matrix3f source) {
		for (int i = 0; i < mValues.length; i++) {
			mValues[i] = source.mValues[i];
		}
        return this;
    }
    
    public void setValue(final int row, final int col, float v) {
    	mValues[pos(row,col)] = v;
    }
    
    public void setValue(final int row, final int col, double v) {
        mValues[pos(row,col)] = (float)v;
    }
    
    public void setValues(float [] values) {
    	mValues = values;
    }
}
