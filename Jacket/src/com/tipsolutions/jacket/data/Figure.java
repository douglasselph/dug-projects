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

import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;

/**
 * Supports all shapes.
 */
public class Figure 
{
	protected Shape mShape;

    public Figure(Shape shape) {
    	mShape = shape;
    }
    
    // Apply the set rotation of the figure.
    public void applyRotation(GL10 gl) {
    }
    
    // Draw figure relative to the current location.
    public void draw(GL10 gl) {
    }
 
}