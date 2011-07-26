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

import com.tipsolutions.jacket.data.Shape;
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
        mCamera = camera;
    }
    
    public void setShape(Shape shape) {
    	mShape = shape;
    }

    public void onDrawFrame(GL10 gl) {
    	super.onDrawFrame(gl);
        
    	if (mShape != null) {
            mShape.onDraw(mGL);
    	}
    }
    
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	super.onSurfaceChanged(gl, width, height);
    }
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    	super.onSurfaceCreated(gl, config);

//        /*
//         * By default, OpenGL enables features that improve quality
//         * but reduce performance. One might want to tweak that
//         * especially on software renderer.
//         */
//        gl.glDisable(GL10.GL_DITHER);
//        /*
//         * Some one-time OpenGL initialization can be made here
//         * probably based on features of this particular context
//         */
//         gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
//
//         if (mTranslucentBackground) {
//             gl.glClearColor(0,0,0,0);
//         } else {
//             gl.glClearColor(1,1,1,1);
//         }
//         gl.glShadeModel(GL10.GL_SMOOTH);
    }
}
