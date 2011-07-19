package com.tipsolutions.jacket.data;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;

public class Shape extends ShapeData {
	Color4f mColor = null;
	protected Vector3f mLoc = null;
	protected Rotate mRotate = new Rotate(0,0,0);
	
	@Override
	public void onDraw(GL10 gl) {
		gl.glPushMatrix();
		
		if (mRotate != null) {
    		mRotate.apply(gl);
		}
		if (mLoc != null) {
			mLoc.apply(gl);
		}
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
	public Rotate getRotate() { return mRotate; }
	public Vector3f getLocation() { return mLoc; }
	
	public void setColor(Color4f color) { mColor = color; }
	public void setRotate(Rotate x) { mRotate = x; }
	public void setLocation(Vector3f x) { mLoc = x; }
	
	public Rotate addRotate(Rotate x) { return mRotate.add(x); }
}
