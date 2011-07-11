package com.tipsolutions.jacket.data;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;

public class Pyramid extends Shape {

	public Pyramid() {
	}
	
	public Pyramid(float base, float height) {
		set4(base, height);
	}
	// Make a four sided pyramid the given base side length
	// and height. The y-axis will serve as the center pole
	// defining the height. The forward face will be parallel
	// to the x-axis.
	//
	// Assumes CCW facing.
	public void set4(float base, final float height) {
		float radians60 = (float) (Math.PI/3);
		float triHeight = FloatMath.sin(radians60) / FloatMath.cos(radians60) * base/2;
		final float baseHalf = base/2;
		final float triHeightHalf = triHeight/2;
		
		setVertexData(new FloatData() {
			public void fill(FloatBuffer buf) {
				buf.put(0).put(height).put(0f);   /* 0: peak */
				buf.put(-baseHalf).put(0f).put(triHeightHalf); /* 1: x-left */
				buf.put(baseHalf).put(0f).put(triHeightHalf); /* 2: x-right */
				buf.put(0f).put(0f).put(-triHeightHalf); /* 3: z-depth */
			};
			public int size() { return 4*3; }
		});
		setIndexData(new ShortData() {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)1).put((short)2); /* face front */
				buf.put((short)3); /* next vertex for base */
				buf.put((short)0); /* next vertex for right face */
				buf.put((short)1); /* next vertex for left face */
			};
			public int size() { return 3+3; }
		}, GL10.GL_TRIANGLE_STRIP);
		
		allocBounds();
		setMinX(-baseHalf);
		setMaxX(baseHalf);
		setMinY(0);
		setMaxY(height);
		setMinZ(-triHeightHalf);
		setMaxZ(triHeightHalf);
	}
	
	// Construct a pyramid with the given width (x dir),
	// depth (z dir) and height (y dir).
	public void set4(float width, float depth, float height) {
	}

}
