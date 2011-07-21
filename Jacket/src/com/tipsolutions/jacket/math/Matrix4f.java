package com.tipsolutions.jacket.math;


public class Matrix4f {
	
    public final static Matrix4f IDENTITY = new Matrix4f(1f, 0, 0, 0, 
    													 0, 1f, 0, 0, 
    													 0, 0, 1f, 0,
    													 0, 0, 0, 1f);
    
    protected final float[][] mData = new float[4][4];
    
    public Matrix4f() {
        this(IDENTITY);
    }
    
    public Matrix4f(
    		float m00, float m01, float m02, float m03, 
    		float m10, float m11, float m12, float m13,
    		float m20, float m21, float m22, float m23,
    		float m30, float m31, float m32, float m33) {
        mData[0][0] = m00;
        mData[0][1] = m01;
        mData[0][2] = m02;
        mData[0][3] = m03;
        mData[1][0] = m10;
        mData[1][1] = m11;
        mData[1][2] = m12;
        mData[1][3] = m13;
        mData[2][0] = m20;
        mData[2][1] = m21;
        mData[2][2] = m22;
        mData[2][3] = m23;
        mData[3][0] = m30;
        mData[3][1] = m31;
        mData[3][2] = m32;
        mData[3][3] = m33;
    }

    public Matrix4f(final Matrix4f source) {
        set(source);
    }

    public float getValue(final int row, final int column) {
        return mData[row][column];
    }
    
    public void setValue(final int row, final int column, float f) {
        mData[row][column] = f;
    }
    
    public Matrix4f set(final Matrix4f source) {
        mData[0][0] = source.getValue(0, 0);
        mData[1][0] = source.getValue(1, 0);
        mData[2][0] = source.getValue(2, 0);
        mData[3][0] = source.getValue(3, 0);

        mData[0][1] = source.getValue(0, 1);
        mData[1][1] = source.getValue(1, 1);
        mData[2][1] = source.getValue(2, 1);
        mData[3][1] = source.getValue(3, 1);

        mData[0][2] = source.getValue(0, 2);
        mData[1][2] = source.getValue(1, 2);
        mData[2][2] = source.getValue(2, 2);
        mData[3][2] = source.getValue(3, 2);
        
        mData[0][3] = source.getValue(0, 3);
        mData[1][3] = source.getValue(1, 3);
        mData[2][3] = source.getValue(2, 3);
        mData[3][3] = source.getValue(3, 3);

        return this;
    }
}
