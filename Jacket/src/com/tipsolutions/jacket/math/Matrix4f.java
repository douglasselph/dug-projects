package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;

import android.opengl.Matrix;

public class Matrix4f {
	
    public final static Matrix4f IDENTITY = new Matrix4f(1f, 0, 0, 0, 
    													 0, 1f, 0, 0, 
    													 0, 0, 1f, 0,
    													 0, 0, 0, 1f);
    
    protected float [] mData = new float[16];
    
    public Matrix4f() {
        this(1, 0, 0, 0,
        	 0, 1, 0, 0,
        	 0, 0, 1, 0,
        	 0, 0, 0, 1);
    }
    
    public Matrix4f(
    		float m00, float m01, float m02, float m03, 
    		float m10, float m11, float m12, float m13,
    		float m20, float m21, float m22, float m23,
    		float m30, float m31, float m32, float m33) {
    	setValue(0, 0, m00);
    	setValue(0, 1, m01);
    	setValue(0, 2, m02);
    	setValue(0, 3, m03);
    	setValue(1, 0, m10);
    	setValue(1, 1, m11);
    	setValue(1, 2, m12);
    	setValue(1, 3, m13);
    	setValue(2, 0, m20);
    	setValue(2, 1, m21);
    	setValue(2, 2, m22);
    	setValue(2, 3, m23);
    	setValue(3, 0, m30);
    	setValue(3, 1, m31);
    	setValue(3, 2, m32);
    	setValue(3, 3, m33);
    }

    public Matrix4f(final Matrix4f source) {
    	if (source == null) {
    		set(IDENTITY);
    	} else {
            set(source);
    	}
    }

    public Matrix4f add(final Matrix4f with) {
    	for (int i = 0; i < mData.length; i++) {
    		mData[i] += with.mData[i];
    	}
        return this;
    }
    
    public void addLocation(final Vector3f loc) {
    	setValue(0, 3, getValue(0, 3) + loc.getX());
    	setValue(1, 3, getValue(1, 3) + loc.getY());
    	setValue(2, 3, getValue(2, 3) + loc.getZ());
    }
    
	public void addRotateDegrees(float angleX, float angleY, float angleZ) {
		addRotate(Math.toDegrees(angleX), Math.toDegrees(angleY), Math.toDegrees(angleZ));
	}
    
	// Radians
	public void addRotate(double angleX, double angleY, double angleZ) {
		addRotate((float) angleX, (float) angleY, (float) angleZ);
	}
    
	// Radians
	public void addRotate(float angleX, float angleY, float angleZ) {
		Quaternion quat = getQuaternion();
		Quaternion rot = new Quaternion().fromAngles(angleX, angleY, angleZ);
		quat.multiply(rot);
		quat.toRotationMatrix(this);
	}
 
    /**
     * Multiplies the given vector by this matrix (v * M). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vector
     *            the vector to multiply this matrix by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vector and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vector is null
     */
    public Vector3f multVM(Vector3f vector, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        double w = 1;

        store.setX(getValue(0,0) * x + getValue(1,0) * y + getValue(2,0) * z + getValue(3,0) * w);
        store.setY(getValue(0,1) * x + getValue(1,1) * y + getValue(2,1) * z + getValue(3,1) * w);
        store.setZ(getValue(0,2) * x + getValue(1,2) * y + getValue(2,2) * z + getValue(3,2) * w);
//        store.setW(getValue(0,3) * x + getValue(1,3) * y + getValue(2,3) * z + getValue(3,3) * w);

        return store;
    }
    
    public Vector3f multVM(Vector3f vector) {
    	return multVM(vector, vector);
    }

    /**
     * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vector
     *            the vector to multiply this matrix by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vector and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vector is null
     */
    public Vector3f multMV(final Vector3f vector, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        double w = 1;

        store.setX(getValue(0,0) * x + getValue(0,1) * y + getValue(0,2) * z + getValue(0,3) * w);
        store.setY(getValue(1,0) * x + getValue(1,1) * y + getValue(1,2) * z + getValue(1,3) * w);
        store.setZ(getValue(2,0) * x + getValue(2,1) * y + getValue(2,2) * z + getValue(2,3) * w);
//        store.setW(_data[3][0] * x + _data[3][1] * y + _data[3][2] * z + _data[3][3] * w);

        return store;
    }
    
    public Vector3f multMV(final Vector3f vector) {
    	return multMV(vector, vector);
    }

    public void setIdentity() {
    	set(IDENTITY);
    }
    
    public float [] getArray() {
    	return mData;
    }
    
    public boolean equals(final Matrix4f other) {
    	for (int i = 0; i < mData.length; i++) {
    		if (mData[i] != other.mData[i]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public FloatBuffer getBuffer() {
    	FloatBuffer fbuf = FloatBuffer.allocate(mData.length);
    	fbuf.put(mData);
    	fbuf.rewind();
    	return fbuf;
    }
    
    public Vector3f getLocation() {
    	return new Vector3f(getValue(0, 3), 
    					    getValue(1, 3), 
    					    getValue(2, 3));
    }
    
    // Not sure enough about matrix mathematics about what it means
    // when the location is stored in the 4th row. For OpenGL() 
    // the location needs to be in the 4th column. So I have to 
    // transpose this once and a while.
    public Vector3f getLocationAlt() {
    	return new Vector3f(getValue(3, 0), 
    					    getValue(3, 1), 
    					    getValue(3, 2));
    }
    
    public Matrix4f transposeLocation() {
    	Matrix4f mat = new Matrix4f();
    	mat.setRotation(getRotation());
    	Vector3f vecLoc = getLocation();
    	Vector3f vecAlt = getLocationAlt();
    	mat.setLocation(vecAlt);
    	mat.setLocationAlt(vecLoc);
    	return mat;
    }
    
    public Quaternion getQuaternion() {
    	return new Quaternion().fromRotationMatrix(this);
    }
    
    public Matrix3f getRotation() {
    	return new Matrix3f(
    			getValue(0,0), getValue(0,1), getValue(0,2),
    			getValue(1,0), getValue(1,1), getValue(1,2),
    			getValue(2,0), getValue(2,1), getValue(2,2));
    }
    
    public float getValue(int row, int col) {
        return mData[col*4+row];
    }
    
    public Matrix4f invert() {
    	Matrix.invertM(mData, 0, mData, 0);
    	return this;
//    	final float dA0 = getValue(0,0) * getValue(1,1) - getValue(0,1) * getValue(1,0);
//    	final float dA1 = getValue(0,0) * getValue(1,2) - getValue(0,2) * getValue(1,0);
//    	final float dA2 = getValue(0,0) * getValue(1,3) - getValue(0,3) * getValue(1,0);
//    	final float dA3 = getValue(0,1) * getValue(1,2) - getValue(0,2) * getValue(1,1);
//    	final float dA4 = getValue(0,1) * getValue(1,3) - getValue(0,3) * getValue(1,1);
//    	final float dA5 = getValue(0,2) * getValue(1,3) - getValue(0,3) * getValue(1,2);
//    	final float dB0 = getValue(2,0) * getValue(3,1) - getValue(2,1) * getValue(3,0);
//    	final float dB1 = getValue(2,0) * getValue(3,2) - getValue(2,2) * getValue(3,0);
//    	final float dB2 = getValue(2,0) * getValue(3,3) - getValue(2,3) * getValue(3,0);
//    	final float dB3 = getValue(2,1) * getValue(3,2) - getValue(2,2) * getValue(3,1);
//    	final float dB4 = getValue(2,1) * getValue(3,3) - getValue(2,3) * getValue(3,1);
//    	final float dB5 = getValue(2,2) * getValue(3,3) - getValue(2,3) * getValue(3,2);
//    	final float det = dA0 * dB5 - dA1 * dB4 + dA2 * dB3 + dA3 * dB2 - dA4 * dB1 + dA5 * dB0;
//
//    	if (Math.abs(det) <= MathUtils.EPSILON) {
//    		throw new ArithmeticException("This matrix cannot be inverted");
//    	}
//    	final float temp00 = +getValue(1,1) * dB5 - getValue(1,2) * dB4 + getValue(1,3) * dB3;
//    	final float temp10 = -getValue(1,0) * dB5 + getValue(1,2) * dB2 - getValue(1,3) * dB1;
//    	final float temp20 = +getValue(1,0) * dB4 - getValue(1,1) * dB2 + getValue(1,3) * dB0;
//    	final float temp30 = -getValue(1,0) * dB3 + getValue(1,1) * dB1 - getValue(1,2) * dB0;
//    	final float temp01 = -getValue(0,1) * dB5 + getValue(0,2) * dB4 - getValue(0,3) * dB3;
//    	final float temp11 = +getValue(0,0) * dB5 - getValue(0,2) * dB2 + getValue(0,3) * dB1;
//    	final float temp21 = -getValue(0,0) * dB4 + getValue(0,1) * dB2 - getValue(0,3) * dB0;
//    	final float temp31 = +getValue(0,0) * dB3 - getValue(0,1) * dB1 + getValue(0,2) * dB0;
//    	final float temp02 = +getValue(3,1) * dA5 - getValue(3,2) * dA4 + getValue(3,3) * dA3;
//    	final float temp12 = -getValue(3,0) * dA5 + getValue(3,2) * dA2 - getValue(3,3) * dA1;
//    	final float temp22 = +getValue(3,0) * dA4 - getValue(3,1) * dA2 + getValue(3,3) * dA0;
//    	final float temp32 = -getValue(3,0) * dA3 + getValue(3,1) * dA1 - getValue(3,2) * dA0;
//    	final float temp03 = -getValue(2,1) * dA5 + getValue(2,2) * dA4 - getValue(2,3) * dA3;
//    	final float temp13 = +getValue(2,0) * dA5 - getValue(2,2) * dA2 + getValue(2,3) * dA1;
//    	final float temp23 = -getValue(2,0) * dA4 + getValue(2,1) * dA2 - getValue(2,3) * dA0;
//    	final float temp33 = +getValue(2,0) * dA3 - getValue(2,1) * dA1 + getValue(2,2) * dA0;
//
//    	set(temp00, temp01, temp02, temp03, 
//    		temp10, temp11, temp12, temp13, 
//    		temp20, temp21, temp22, temp23,
//    		temp30, temp31, temp32, temp33);
//    	return mult(1.0 / det);
    }

    public Matrix4f mult(final Matrix4f matrix) {
//    	Matrix.multiplyMM(mData, 0, mData, 0, matrix.mData, 0);
//    	return this;
    	float data00 = getValue(0,0);
    	float data01 = getValue(0,1);
    	float data02 = getValue(0,2);
    	float data03 = getValue(0,3);
    	float data10 = getValue(1,0);
    	float data11 = getValue(1,1);
    	float data12 = getValue(1,2);
    	float data13 = getValue(1,3);
    	float data20 = getValue(2,0);
    	float data21 = getValue(2,1);
    	float data22 = getValue(2,2);
    	float data23 = getValue(2,3);
    	float data30 = getValue(3,0);
    	float data31 = getValue(3,1);
    	float data32 = getValue(3,2);
    	float data33 = getValue(3,3);
    	
    	float m00 = matrix.getValue(0,0);
    	float m01 = matrix.getValue(0,1);
    	float m02 = matrix.getValue(0,2);
    	float m03 = matrix.getValue(0,3);
    	float m10 = matrix.getValue(1,0);
    	float m11 = matrix.getValue(1,1);
    	float m12 = matrix.getValue(1,2);
    	float m13 = matrix.getValue(1,3);
    	float m20 = matrix.getValue(2,0);
    	float m21 = matrix.getValue(2,1);
    	float m22 = matrix.getValue(2,2);
    	float m23 = matrix.getValue(2,3);
    	float m30 = matrix.getValue(3,0);
    	float m31 = matrix.getValue(3,1);
    	float m32 = matrix.getValue(3,2);
    	float m33 = matrix.getValue(3,3);
    	
        double temp00 = data00 * m00 + data01 * m10 + 
        			    data02 * m20 + data03 * m30;
        double temp01 = data00 * m01 + data01 * m11 + 
        			    data02 * m21 + data03 * m31;
        double temp02 = data00 * m02 + data01 * m12 + 
        			    data02 * m22 + data03 * m32;
        double temp03 = data00 * m03 + data01 * m13 + 
        				data02 * m23 + data03 * m33;

        double temp10 = data10 * m00 + data11 * m10 + 
        			    data12 * m20 + data13 * m30;
        double temp11 = data10 * m01 + data11 * m11 + 
        			    data12 * m21 + data13 * m31;
        double temp12 = data10 * m02 + data11 * m12 + 
        				data12 * m22 + data13 * m32;
        double temp13 = data10 * m03 + data11 * m13 + 
        			    data12 * m23 + data13 * m33;

        double temp20 = data20 * m00 + data21 * m10 + 
        			    data22 * m20 + data23 * m30;
        double temp21 = data20 * m01 + data21 * m11 + 
        			    data22 * m21 + data23 * m31;
        double temp22 = data20 * m02 + data21 * m12 + 
        				data22 * m22 + data23 * m32;
        double temp23 = data20 * m03 + data21 * m13 + 
        				data22 * m23 + data23 * m33;

        double temp30 = data30 * m00 + data31 * m10 + 
        			    data32 * m20 + data33 * m30;
        double temp31 = data30 * m01 + data31 * m11 + 
        			    data32 * m21 + data33 * m31;
        double temp32 = data30 * m02 + data31 * m12 + 
        			    data32 * m22 + data33 * m32;
        double temp33 = data30 * m03 + data31 * m13 + 
        			    data32 * m23 + data33 * m33;

        set(temp00, temp01, temp02, temp03, 
        	temp10, temp11, temp12, temp13, 
        	temp20, temp21, temp22, temp23,
            temp30, temp31, temp32, temp33);
        return this;
    }
    
    public Matrix4f mult(final double scalar) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                setValue(i,j,getValue(i,j) * scalar);
            }
        }
        return this;
    }
    
    public void setValue(final int row, final int col, float f) {
        mData[col*4+row] = f; // col major/row minor to match OpenGL loadMatrix()
    }
    
    public void setValue(final int row, final int col, double v) {
        mData[col*4+row] = (float) v;
    }
    
    public void set(
    		float m00, float m01, float m02, float m03, 
    		float m10, float m11, float m12, float m13,
    		float m20, float m21, float m22, float m23,
    		float m30, float m31, float m32, float m33) {
    	setValue(0, 0, m00);
    	setValue(0, 1, m01);
    	setValue(0, 2, m02);
    	setValue(0, 3, m03);
    	setValue(1, 0, m10);
    	setValue(1, 1, m11);
    	setValue(1, 2, m12);
    	setValue(1, 3, m13);
    	setValue(2, 0, m20);
    	setValue(2, 1, m21);
    	setValue(2, 2, m22);
    	setValue(2, 3, m23);
    	setValue(3, 0, m30);
    	setValue(3, 1, m31);
    	setValue(3, 2, m32);
    	setValue(3, 3, m33);
    }
    
    public void set(
    		double m00, double m01, double m02, double m03, 
    		double m10, double m11, double m12, double m13,
    		double m20, double m21, double m22, double m23,
    		double m30, double m31, double m32, double m33) {
    	setValue(0, 0, m00);
    	setValue(0, 1, m01);
    	setValue(0, 2, m02);
    	setValue(0, 3, m03);
    	setValue(1, 0, m10);
    	setValue(1, 1, m11);
    	setValue(1, 2, m12);
    	setValue(1, 3, m13);
    	setValue(2, 0, m20);
    	setValue(2, 1, m21);
    	setValue(2, 2, m22);
    	setValue(2, 3, m23);
    	setValue(3, 0, m30);
    	setValue(3, 1, m31);
    	setValue(3, 2, m32);
    	setValue(3, 3, m33);
    }
    
    public Matrix4f set(final Matrix4f source) {
    	for (int i = 0; i < mData.length; i++) {
    		mData[i] = source.mData[i];
    	}
        return this;
    }
    
    public Matrix4f setValues(float [] values) {
    	mData = values;
    	return this;
    }
    
    public void setLocation(final Vector3f v) {
    	setValue(0, 3, v.getX());
    	setValue(1, 3, v.getY());
    	setValue(2, 3, v.getZ());
    	setValue(3, 3, 1);
    }
    
    public void setLocationAlt(final Vector3f v) {
    	setValue(3, 0, v.getX());
    	setValue(3, 1, v.getY());
    	setValue(3, 2, v.getZ());
    	setValue(3, 3, 1);
    }
    
    // Set rotation part of this matrix, leaving everything else as is.
    public void setRotation(final Matrix3f rotate) {
    	for (int row = 0; row < 3; row++) {
        	for (int col = 0; col < 3; col++) {
        		setValue(row, col, rotate.getValue(row, col));
        	}
    	}
    }
    
    public void setRotate(double angleX, double angleY, double angleZ) {
    	/*
        	 |  0  1  2  3 |
        M =  |  4  5  6  7 |
             |  8  9 10 11 |
             | 12 13 14 15 |
        */
        double Cx       = Math.cos(angleX);
        double Sx       = Math.sin(angleX);
        double Cy       = Math.cos(angleY);
        double Sy       = Math.sin(angleY);
        double Cz       = Math.cos(angleZ);
        double Sz       = Math.sin(angleZ);

        double CxSy      = Cx * Sy;
        double SxSy      = Sx * Sy;

        setValue(0, 0, Cy * Cz); 	/*mat[0]*/
        setValue(0, 1, Cy * -Sz);   /*mat[1]*/
        setValue(0, 2, Sy);	        /*mat[2]*/ /* was -Sy */
        setValue(1, 0, SxSy * Cz + Cx * Sz);/*mat[4]*/ /* was -SxSy */
        setValue(1, 1, -SxSy * Sz + Cx * Cz); /*mat[5]*/ /* was SxSy */
        setValue(1, 2, -Sx * Cy); 		/*mat[6]*/
        setValue(2, 0, -CxSy * Cz + Sx * Sz); /*mat[8]*/ /* was CxSy */
        setValue(2, 1, CxSy * Sz + Sx * Cz);/*mat[9]*/ /* was -CxSy */
        setValue(2, 2, Cx * Cy); 		/*mat[10]*/

        // mat[3] =  mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0;
        setValue(0, 3, 0);
        setValue(1, 3, 0);
        setValue(2, 3, 0);
        setValue(3, 0, 0);
        setValue(3, 1, 0);
        setValue(3, 2, 0);
        
        setValue(3, 3, 1); /* mat[15]*/
    }

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[");
		for (int row = 0; row < 4; row++) {
			if (row > 0) {
    			sbuf.append("\n");
			}
			for (int col = 0; col < 4; col++) {
				if (col > 0) {
					sbuf.append(",");
				}
				sbuf.append(getValue(row,col));
			}
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
// Note: this yields inaccuracies due to gimbal lock
//  public void addRotate(final Rotate rotate) {
//  	Rotate cur = getRotate();
//  	Rotate post = new Rotate(cur).add(rotate);
//  	setRotate(getRotate().add(rotate));
//  }

	// Note: Uses Quaternion to avoid the inaccuracies that surface
	// using with the getRotate() function (gimball lock problems).
//  public void addRotateQuat(final Rotate rotate) {
//  	Matrix3f rotMatrix = getRotationMatrix();
//  	Quaternion quatRot = new Quaternion();
//  	quatRot.fromRotationMatrix(rotMatrix);
//  	Rotate curRotate = quatRot.getRotate();
//  	curRotate.add(rotate);
//  	quatRot.set(curRotate);
//  	rotMatrix = quatRot.toRotationMatrix3f();
//  	setRotation(rotMatrix);
//  }
  
    // Gets the rotation using "direct" methods because it is very
    // accurate for most values. For values that are subject to 
    // "gimball lock", use the Quaternion approach.
//    public Rotate getRotate() {
//    	/*
//        	 |  0  1  2  3 |
//        M =  |  4  5  6  7 |
//             |  8  9 10 11 |
//             | 12 13 14 15 |
//        */
//    	double angle_x, angle_y, angle_z;
//    	double Cy;
////    	double tr_x, tr_y;
//    	double verify;
//    	
//    	angle_y = Math.asin(getValue(0,2));/* Calculate Y-axis angle */ /* was -asin(m[2]) */
//    	Cy      = Math.cos( angle_y );
//
//    	if (Math.abs( Cy ) <= 0.005) {/* Gimball lock? */
//    		return getRotateQuat();
//    	}
//    	// Two ways of getting angle_z, should yield the same result */
//    	angle_z = Math.asin(-getValue(0,1)/Cy);
//    	verify = Math.acos(getValue(0,0)/Cy);
//    	
//    	// Two of getting angle_x, should yield the same result */
//    	angle_x = Math.asin(-getValue(1,2)/Cy);
//    	verify = Math.acos(getValue(2,2)/Cy);
//    	
//    	// OLD WAY from the web didn't work that well:
////    	tr_x      =  getValue(2,2) /*mat[10]*// Cy; /* No, so get X-axis angle */
////    	tr_y      = -getValue(1,2) /*mat[6]*// Cy;
////
////    	angle_x  = Math.atan2( tr_y, tr_x );
////
////    	tr_x      =  getValue(0,0)/*mat[0]*/ / Cy; /* Get Z-axis angle */
////    	tr_y      = -getValue(0,1)/*mat[1]*/ / Cy;
////
////    	angle_z = Math.atan2( tr_y, tr_x );
//    	
//    	angle_x = MathUtils.clamp( angle_x );
//    	angle_y = MathUtils.clamp( angle_y );
//    	angle_z = MathUtils.clamp( angle_z );
//    	
//    	return new Rotate((float) angle_x, (float) angle_y, (float) angle_z);
//    }
    
    // A "standard" way of determining the rotations.
    // this way can lead to inaccuracies for certain values 
    // because of "gimball lock".
//    public Rotate getRotateDirect() {
//    	/*
//        	 |  0  1  2  3 |
//        M =  |  4  5  6  7 |
//             |  8  9 10 11 |
//             | 12 13 14 15 |
//        */
//    	double angle_x, angle_y, angle_z;
//    	double Cy;
//    	double tr_x, tr_y;
//    	
//    	angle_y = Math.asin(getValue(0,2));/* Calculate Y-axis angle */
//    	Cy      = Math.cos(angle_y);
//
//    	if (Math.abs( Cy ) > 0.005)             /* Gimball lock? */
//    	{
//    		angle_x = Math.asin(-getValue(1,2)/Cy);
//    		angle_z = Math.asin(-getValue(0,1)/Cy);
//    	
//    		// OLD WAY didn't work that well.
////    		tr_x      =  getValue(2,2) /*mat[10]*// Cy; /* No, so get X-axis angle */
////    		tr_y      = -getValue(1,2) /*mat[6]*// Cy;
//
////    		angle_x  = Math.atan2( tr_y, tr_x );
//
////    		tr_x      =  getValue(0,0)/*mat[0]*/ / Cy; /* Get Z-axis angle */
////    		tr_y      = -getValue(0,1)/*mat[1]*/ / Cy;
//
////    		angle_z  = Math.atan2( tr_y, tr_x );
//    	} else { /* Gimball lock has occurred */
//    		angle_x  = 0;                      /* Set X-axis angle to zero */
//
//    		tr_x      = getValue(1,1)/*mat[5]*/; /* And calculate Z-axis angle */
//    		tr_y      = getValue(1,0)/*mat[4]*/;
//
//    		angle_z  = Math.atan2( tr_y, tr_x );
//    	}
//    	angle_x = MathUtils.clamp( angle_x );
//    	angle_y = MathUtils.clamp( angle_y );
//    	angle_z = MathUtils.clamp( angle_z );
//    	
//    	return new Rotate((float) angle_x, (float) angle_y, (float) angle_z);
//    }
//    
//    public Rotate getRotateQuat() {
//    	return getQuaternion().getRotate();
//    }
    
}
