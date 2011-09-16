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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.shape.Shape;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

/**
 * Render a pair of tumbling cubes.
 */

class MyRenderer extends ControlRenderer {
    boolean mTranslucentBackground;
    Shape mShape = null;
    
    public MyRenderer(ControlSurfaceView view, Shape shape, ControlCamera camera, boolean useTranslucentBackground) {
    	super(view, camera);
        mTranslucentBackground = useTranslucentBackground;
        mShape = shape;
    }
    
    public void setShape(Shape shape) {
    	mShape = shape;
    }
    
    public void onDrawFrame(MatrixTrackingGL gl) {
    	if (mShape != null) {
            mShape.onDraw(gl);
    	}
    }
    
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	super.onSurfaceChanged(gl, width, height);
    }
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    	super.onSurfaceCreated(gl, config);
    }
}
