package com.tipsolutions.jacket.data;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;

public class Shape extends ShapeData {
	Color4f mColor = null;
	Matrix4f mMatrixMod = null;
	
	@Override
	public void onDraw(GL10 gl) {
		gl.glPushMatrix();
		
		if (!hasColorArray()) {
			Color4f color = getColor();
			if (color != null) {
		        gl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			}
		}
		super.onDraw(gl);
		
        gl.glPopMatrix();
	}
	
	protected Color4f _getColor4() { return null; } // Override in super class
	public Color4f getColor() { return mColor != null ? mColor : _getColor4(); }
	public void setColor(Color4f color) { mColor = color; }
	
	@Override 
	protected Matrix4f getMatrix() {
		if (mMatrixMod != null) {
			return mMatrixMod;
		}
		return super.getMatrix();
	}
	
	protected Matrix4f getMatrixMod() {
		if (mMatrixMod == null) {
			mMatrixMod = new Matrix4f(mMatrix);
		}
		return mMatrixMod;
	}
	
	// Warning: subject to inaccuracies
	public Rotate getRotate() { 
		return getMatrix().getRotate(); 
	}
	
	public Vector3f getLocation() { 
		return getMatrix().getLocation(); 
	}
	
	public void setRotate(Rotate x) { 
		getMatrixMod().setRotate(x);
	}
	
	public void setLocation(Vector3f x) { 
		getMatrixMod().setLocation(x);
	}
	
	public void addRotate(Rotate x) {
		getMatrixMod().addRotateQuat(x);
	}
}
