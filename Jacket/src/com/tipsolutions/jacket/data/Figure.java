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

package com.tipsolutions.jacket.data;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;

/**
 * Supports all shapes.
 */
public class Figure 
{
	protected Vector3f mLoc = new Vector3f();
	protected Rotate mRotate = new Rotate();
	protected ShapeData mShape;

    public Figure(ShapeData shape) {
    	mShape = shape;
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
        
        mShape.draw(gl);
    }
    
	public void setLocation(Vector3f x) { mLoc = x; }
	public void setRotation(Rotate x) { mRotate = x; }
	
	public final Vector3f getLocation() { return mLoc; }
}
