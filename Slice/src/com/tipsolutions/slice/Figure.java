/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tipsolutions.slice;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.tipsolutions.jacket.data.FigureData;
import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;

/**
 * A vertex shaded cube.
 */
class Figure extends FigureData
{
	Vector3f mLoc = new Vector3f();
	Rotate mRotate = new Rotate();
	
    public Figure() {
    }

    public void draw(GL10 gl)
    {
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(mLoc.getX(), mLoc.getY(), mLoc.getZ());
		
		if (mRotate.mYaw != 0) {
			gl.glRotatef(mRotate.mYaw, 1, 0, 0);
		}
		if (mRotate.mPitch != 0) {
			gl.glRotatef(mRotate.mPitch, 0, 1, 0);
		}
		if (mRotate.mRoll != 0) {
			gl.glRotatef(mRotate.mRoll, 0, 0, 1);
		}
        gl.glFrontFace(GL10.GL_CW);
        
        if (Main.LOG) {
        	Log.d(Main.TAG, "draw():");
        	Log.d(Main.TAG, "  " + toString("vertexbuf=", mVertexBuf.asFloatBuffer()));
        	Log.d(Main.TAG, "  " + toString("indexbuf=", mIndexBuf.asShortBuffer()));
        }
        if (mVertexBuf != null) {
        	gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuf.asFloatBuffer());
        }
        if (mNormalBuf != null) {
        	gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuf.asFloatBuffer());
        }
        if (mColorBuf != null) {
        	gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
        }
        if (mIndexBuf != null) {
            gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount, GL10.GL_UNSIGNED_SHORT, mIndexBuf.asShortBuffer());
        }
    }
    
	public float getLenX() { return getMaxX()-getMinX(); }
	public float getLenY() { return getMaxY()-getMinY(); }
	public float getLenZ() { return getMaxZ()-getMinZ(); }
	
	public void setLocation(Vector3f x) { mLoc = x; }
	public void setRotation(Rotate x) { mRotate = x; }
	
	public final Vector3f getLocation() { return mLoc; }
}
