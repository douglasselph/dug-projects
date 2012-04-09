package com.tipsolutions.jacket.shape;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Vector3f;

public class Box extends Shape {

	public Box() {
		set(1f, 1f, 1f, null);
	}
	
	public Box(float length) {
		set(length, length, length, null);
	}
	
	public Box(float length, Texture texture) {
		set(length, length, length, texture);
	}
	
	public Box(float xlength, float ylength, float zlength) {
		set(xlength, ylength, zlength, null);
	}
	
	public Box(float xlength, float ylength, float zlength, Texture texture) {
		set(xlength, ylength, zlength, texture);
	}
	
	// Make a centered cube with the indicated length.
	// Assumes CCW facing.
	public void set(float xlength, float ylength, float zlength, final Texture texture) {
		final float xdelta = xlength/2;
		final float ydelta = ylength/2;
		final float zdelta = zlength/2;
		
		setVertexData(new dFloatBuf() {
			public void fill(FloatBuffer buf) {
        		// 0: lower-left back 
        		// 1: lower-right back 
        		// 2: upper-right back
        		// 3: upper-left back
        		// 4: lower-left front
        		// 5: lower-right front
        		// 6: upper-right front
        		// 7: upper-left front
				Vector3f verts[] = new Vector3f[8];
				
				verts[0] = new Vector3f(-xdelta, -ydelta, -zdelta); // 0: lower-left back 
				verts[1] = new Vector3f(xdelta, -ydelta, -zdelta);  // 1: lower-right back 
				verts[2] = new Vector3f(xdelta, ydelta, -zdelta);   // 2: upper-right back
				verts[3] = new Vector3f(-xdelta, ydelta, -zdelta);  // 3: upper-left back
				verts[4] = new Vector3f(-xdelta, -ydelta, zdelta);  // 4: lower-left front
				verts[5] = new Vector3f(xdelta, -ydelta, zdelta);   // 5: lower-right front
				verts[6] = new Vector3f(xdelta, ydelta, zdelta);    // 6: upper-right front
				verts[7] = new Vector3f(-xdelta, ydelta, zdelta);   // 7: upper-left front
				
				// Back
				verts[0].put(buf); // 0: lower-left back
				verts[1].put(buf); // 1: lower-right back
				verts[2].put(buf); // 2: upper-right back
				verts[3].put(buf); // 3: upper-left back
				
				// Right
				verts[1].put(buf); // 4: lower-right back
				verts[2].put(buf); // 5: upper-right back
				verts[5].put(buf); // 6: lower-right front
				verts[6].put(buf); // 7: upper-right front
				
				// Front
				verts[4].put(buf); // 8: lower-left front
				verts[5].put(buf); // 9: lower-right front
				verts[6].put(buf); // 10: upper-right front
				verts[7].put(buf); // 11: upper-left front
				
				// Left
				verts[0].put(buf); // 12: lower-left back
				verts[3].put(buf); // 13: upper-left back
				verts[4].put(buf); // 14: lower-left front
				verts[7].put(buf); // 15: upper-left front
				
				// Top
				verts[2].put(buf); // 16: upper-right back
				verts[3].put(buf); // 17: upper-left back
				verts[6].put(buf); // 18: upper-right front
				verts[7].put(buf); // 19: upper-left front
				
				// Bottom
				verts[0].put(buf); // 20: lower-left back
				verts[1].put(buf); // 21: lower-right back
				verts[4].put(buf); // 22: lower-left front
				verts[5].put(buf); // 23: lower-right front
			}
			
			public int size() { 
				return 4*3*6;
			}
		});
		
		setNormalData(new dFloatBuf() {
			public void fill(FloatBuffer buf) {
				int i;
				
				// Back
				for (i = 0; i < 4; i++) {
    				buf.put(0).put(0).put(-1);
				}
				// Right
				for (i = 0; i < 4; i++) {
    				buf.put(1).put(0).put(0);
				}
				// Front
				for (i = 0; i < 4; i++) {
    				buf.put(0).put(0).put(1);
				}
				// Left
				for (i = 0; i < 4; i++) {
    				buf.put(-1).put(0).put(0);
				}
				// Top
				for (i = 0; i < 4; i++) {
    				buf.put(0).put(1).put(0);
				}
				// Bottom
				for (i = 0; i < 4; i++) {
    				buf.put(0).put(-1).put(0);
				}
			};
			
			public int size() { 
				return (4*3*6);
			}
		});
		
		setIndexData(new dShortBuf() {
			public void fill(ShortBuffer buf) {
				// CCW:
				
				// Back
        		// 0: lower-left back 
        		// 1: lower-right back 
        		// 2: upper-right back
        		// 3: upper-left back
				buf.put((short)0).put((short)2).put((short)1);
				buf.put((short)3).put((short)2).put((short)0);
				
				// Right
        		// 4: lower-right back 
        		// 5: upper-right back
        		// 6: lower-right front
        		// 7: upper-right front
				buf.put((short)6).put((short)4).put((short)5);
				buf.put((short)6).put((short)5).put((short)7);
				
				// Front
        		// 8: lower-left front
        		// 9: lower-right front
        		// 10: upper-right front
        		// 11: upper-left front
				buf.put((short)8).put((short)9).put((short)10);
				buf.put((short)8).put((short)10).put((short)11);
				
				// Left
        		// 12: lower-left back 
        		// 13: upper-left back
        		// 14: lower-left front
        		// 15: upper-left front
				buf.put((short)12).put((short)14).put((short)15);
				buf.put((short)12).put((short)15).put((short)13);
				
				// Top
        		// 16: upper-right back
        		// 17: upper-left back
        		// 18: upper-right front
        		// 19: upper-left front
				buf.put((short)19).put((short)18).put((short)16);
				buf.put((short)19).put((short)16).put((short)17);
				
				// Bottom
        		// 20: lower-left back 
        		// 21: lower-right back 
        		// 22: lower-left front
        		// 23: lower-right front
				buf.put((short)20).put((short)21).put((short)23);
				buf.put((short)20).put((short)23).put((short)22);
			};
			public int size() { return 6*2*3; }
		});
		if (texture != null) {
			setTexture(texture);
    		setTextureData(new dFloatBuf() {
    			public void fill(FloatBuffer buf) {
    				// Back
    				// lower-left back 
    				// lower-right back 
    				// upper-right back
    				// upper-left back
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(0f).put(0f); // was 0,0
    				buf.put(0f).put(1f); // was 0,1
    				buf.put(1f).put(1f); // was 1,1

    				// Right
    				// lower-right back 
    				// upper-right back
    				// lower-right front
    				// upper-right front
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(1f).put(1f); // was 1,1
    				buf.put(0f).put(0f); // was 0,0
    				buf.put(0f).put(1f); // was 0,1

    				// Front
    				// lower-left front
    				// lower-right front
    				// upper-right front
    				// upper-left front
    				buf.put(0f).put(0f); // was 0,0
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(1f).put(1f); // was 1,1
    				buf.put(0f).put(1f); // was 0,1

    				// Left
    				// lower-left back 
    				// upper-left back
    				// lower-left front
    				// upper-left front
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(1f).put(1f); // was 1,1
    				buf.put(0f).put(0f); // was 0,0
    				buf.put(0f).put(1f); // was 0,1

    				// Top
    				// upper-right back
    				// upper-left back
    				// upper-right front
    				// upper-left front
    				buf.put(1f).put(1f); // was 1,1
    				buf.put(0f).put(1f); // was 0,1
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(0f).put(0f); // was 0,0

    				// Bottom
    				// lower-left back 
    				// lower-right back 
    				// lower-left front
    				// lower-right front
    				buf.put(0f).put(0f); // was 0,0
    				buf.put(1f).put(0f); // was 1,0
    				buf.put(0f).put(1f); // was 0,1
    				buf.put(1f).put(1f); // was 1,1
    			};
    			public int size() { return 6*4*2; }
    		});
		}
		mBounds.setMinX(-xdelta);
		mBounds.setMaxX(xdelta);
		mBounds.setMinY(-ydelta);
		mBounds.setMaxY(ydelta);
		mBounds.setMinZ(-zdelta);
		mBounds.setMaxZ(zdelta);
	}
}
