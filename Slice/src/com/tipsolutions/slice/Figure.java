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
        gl.glFrontFace(gl.GL_CW);
        
        if (mVertexBuf != null) {
            gl.glVertexPointer(3, gl.GL_FLOAT, 0, mVertexBuf.asFloatBuffer());
        }
        if (mNormalBuf != null) {
            gl.glNormalPointer(gl.GL_FLOAT, 0, mNormalBuf.asFloatBuffer());
        }
        if (mColorBuffer != null) {
            gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer.asShortBuffer());
        }
        if (mIndexBuf != null) {
            gl.glDrawElements(gl.GL_TRIANGLES, mIndexCount, gl.GL_UNSIGNED_SHORT, mIndexBuf.asShortBuffer());
        }
    }
    
	public float getLenX() { return getMaxX()-getMinX(); }
	public float getLenY() { return getMaxY()-getMinY(); }
	public float getLenZ() { return getMaxZ()-getMinZ(); }
	
	public void setLocation(Vector3f x) { mLoc = x; }
	public void setRotation(Rotate x) { mRotate = x; }
	
	public final Vector3f getLocation() { return mLoc; }
}
