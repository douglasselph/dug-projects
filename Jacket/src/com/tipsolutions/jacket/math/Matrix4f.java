package com.tipsolutions.jacket.math;

import android.util.Log;



public class Matrix4f {
	
    public final static Matrix4f IDENTITY = new Matrix4f(1f, 0, 0, 0, 
    													 0, 1f, 0, 0, 
    													 0, 0, 1f, 0,
    													 0, 0, 0, 1f);
    
    protected final float [] mData = new float[16];
    
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
    	setValue(3, 0, getValue(3, 0) + loc.getX());
    	setValue(3, 1, getValue(3, 1) + loc.getY());
    	setValue(3, 2, getValue(3, 2) + loc.getZ());
    }
    
    // Note: this yields inaccuracies due to gimbal lock
    public void addRotate(final Rotate rotate) {
    	setRotate(getRotate().add(rotate));
    }

	// Note: Uses Quaternion to avoid the inaccuracies that surface
	// using with the getRotate() function (gimball lock problems).
    public void addRotateQuat(final Rotate rotate) {
    	Matrix3f rotMatrix = getRotationMatrix();
    	Quaternion quatRot = new Quaternion();
    	quatRot.fromRotationMatrix(rotMatrix);
    	Log.d("DEBUG", "addRotateQuat(), quat initial=" + quatRot.toString());
    	Rotate curRotate = quatRot.getRotate();
    	Log.d("DEBUG", "addRotateQuat(), rotate was =" + curRotate.toString());
    	curRotate.add(rotate);
    	Log.d("DEBUG", "addRotateQuat(), rotate now =" + curRotate.toString());
    	quatRot.set(curRotate);
    	Log.d("DEBUG", "addRotateQuat(), quat post=" + quatRot.toString());
    	Log.d("DEBUG", " ...post=" + quatRot.getRotate().toString());
    	rotMatrix = quatRot.toRotationMatrix3f();
    	setRotation(rotMatrix);
    }
    
    public void setIdentity() {
    	set(IDENTITY);
    }
    
    public float [] getArray() {
    	return mData;
    }
    
    public Vector3f getLocation() {
    	return new Vector3f(getValue(0, 3), 
    					    getValue(1, 3), 
    					    getValue(2, 3));
    }
    
    public Rotate getRotate() {
    	/*
        	 |  0  1  2  3 |
        M =  |  4  5  6  7 |
             |  8  9 10 11 |
             | 12 13 14 15 |
        */
    	double angle_x, angle_y, angle_z;
    	double C;
    	double tr_x, tr_y;
    	
    	angle_y = -Math.asin(getValue(0,2)/*mat[2]*/);/* Calculate Y-axis angle */
    	C       =  Math.cos( angle_y );

    	if (Math.abs( C ) > 0.005)             /* Gimball lock? */
    	{
    		tr_x      =  getValue(2,2) /*mat[10]*// C; /* No, so get X-axis angle */
    		tr_y      = -getValue(1,2) /*mat[6]*// C;

    		angle_x  = Math.atan2( tr_y, tr_x );

    		tr_x      =  getValue(0,0)/*mat[0]*/ / C; /* Get Z-axis angle */
    		tr_y      = -getValue(0,1)/*mat[1]*/ / C;

    		angle_z  = Math.atan2( tr_y, tr_x );
    	} else { /* Gimball lock has occurred */
    		angle_x  = 0;                      /* Set X-axis angle to zero */

    		tr_x      = getValue(1,1)/*mat[5]*/; /* And calculate Z-axis angle */
    		tr_y      = getValue(1,0)/*mat[4]*/;

    		angle_z  = Math.atan2( tr_y, tr_x );
    	}
    	angle_x = MathUtils.clamp( angle_x );
    	angle_y = MathUtils.clamp( angle_y );
    	angle_z = MathUtils.clamp( angle_z );
    	
    	return new Rotate((float) angle_x, (float) angle_y, (float) angle_z);
    }
    
    public Rotate getRotateQuat() {
    	return getQuaternion().getRotate();
    }
    
    public Quaternion getQuaternion() {
    	Quaternion quat = new Quaternion();
    	quat.fromRotationMatrix(this);
    	return quat;
    }
    
    public Matrix3f getRotationMatrix() {
    	return new Matrix3f(
    			getValue(0,0), getValue(0,1), getValue(0,2),
    			getValue(1,0), getValue(1,1), getValue(1,2),
    			getValue(2,0), getValue(2,1), getValue(2,2));
    }
    
    public float getValue(int row, int col) {
        return mData[col*4+row];
    }
    
    public Matrix4f mult(final Matrix4f matrix) {
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
    
    public void setLocation(final Vector3f v) {
    	setValue(0, 3, v.getX());
    	setValue(1, 3, v.getY());
    	setValue(2, 3, v.getZ());
    }
    
    // Set rotation part of this matrix, leaving everything else as is.
    public void setRotation(final Matrix3f rotate) {
    	for (int row = 0; row < 3; row++) {
        	for (int col = 0; col < 3; col++) {
        		setValue(row, col, rotate.getValue(row, col));
        	}
    	}
    }
    
    public void setRotate(final Rotate rotate) {
    	/*
        	 |  0  1  2  3 |
        M =  |  4  5  6  7 |
             |  8  9 10 11 |
             | 12 13 14 15 |
        */
        double A       = Math.cos(rotate.getAngleX());
        double B       = Math.sin(rotate.getAngleX());
        double C       = Math.cos(rotate.getAngleY());
        double D       = Math.sin(rotate.getAngleY());
        double E       = Math.cos(rotate.getAngleZ());
        double F       = Math.sin(rotate.getAngleZ());

        double AD      = A * D;
        double BD      = B * D;

        setValue(0, 0, C * E); 	/*mat[0]*/
        setValue(0, 1, -C * F); /*mat[1]*/
        setValue(0, 2, -D);	    /*mat[2]*/
        setValue(1, 0, -BD * E + A * F);/*mat[4]*/
        setValue(1, 1, BD * F + A * E); /*mat[5]*/
        setValue(1, 2, -B * C); 		/*mat[6]*/
        setValue(2, 0, AD * E + B * F); /*mat[8]*/
        setValue(2, 1, -AD * F + B * E);/*mat[9]*/
        setValue(2, 2, A * C); 		/*mat[10]*/

        // mat[3] =  mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0;
        setValue(0, 3, 0);
        setValue(1, 3, 0);
        setValue(2, 3, 0);
        setValue(3, 0, 0);
        setValue(3, 1, 0);
        setValue(3, 2, 0);
        
        setValue(3, 3, 1); /* mat[15]*/
    }
}
