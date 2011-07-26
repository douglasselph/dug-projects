package com.tipsolutions.jacket.data;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class Shape extends ShapeData {
	Color4f mColor = null;
	Matrix4f mMatrixMod = null;
	
	public void addRotateDegrees(float angleX, float angleY, float angleZ) {
		addRotate(Math.toDegrees(angleX), Math.toDegrees(angleY), Math.toDegrees(angleZ));
	}
	
	// Radians
	public void addRotate(float angleX, float angleY, float angleZ) {
		Quaternion quat = getQuaternionMod();
		Quaternion rot = new Quaternion().fromAngles(angleX, angleY, angleZ);
		quat.multiply(rot);
		quat.toRotationMatrix(mMatrixMod);
	}
	
	public void addRotate(double angleX, double angleY, double angleZ) {
		addRotate((float) angleX, (float) angleY, (float) angleZ);
	}
	
	@Override
	public void onDraw(MatrixTrackingGL gl) {
		if (!hasColorArray()) {
			Color4f color = getColor();
			if (color != null) {
		        gl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			}
		}
		super.onDraw(gl);
	}
	
	protected Color4f _getColor4() { return null; } // Override in super class
	public Color4f getColor() { return mColor != null ? mColor : _getColor4(); }
	public void setColor(Color4f color) { mColor = color; }
	
	// Returns the currently active matrix that should be applied for drawing.
	// Warning: this can return NULL.
	@Override 
	protected Matrix4f getMatrix() {
		if (mMatrixMod != null) {
			return mMatrixMod;
		}
		return super.getMatrix();
	}
	
	// Get the modification matrix that lives on top of the object matrix.
	// Will never return null.
	protected Matrix4f getMatrixMod() {
		if (mMatrixMod == null) {
			mMatrixMod = new Matrix4f(mMatrix);
		}
		return mMatrixMod;
	}
	
	public Quaternion getQuaternionMod() { 
		return getMatrixMod().getQuaternion();
	}
	
	public Vector3f getLocationMod() { 
		return getMatrixMod().getLocation(); 
	}
	
	public void setLocation(Vector3f x) { 
		getMatrixMod().setLocation(x);
	}
}
