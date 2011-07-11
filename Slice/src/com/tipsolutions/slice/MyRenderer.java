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

import com.tipsolutions.jacket.data.Figure;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.view.CameraControl;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;

/**
 * Render a pair of tumbling cubes.
 */

class MyRenderer extends ControlRenderer {
	
    boolean mTranslucentBackground;
    Shape mShape;
    final CameraControl mCamera;

    public MyRenderer(ControlSurfaceView view, Shape shape, CameraControl camera, boolean useTranslucentBackground) {
    	super(view);
        mTranslucentBackground = useTranslucentBackground;
        mShape = shape;
        mCamera = camera;
    }

    // DEBUG
    float _red = 0.9f;
    float _green = 0.2f;
    float _blue = 0.2f;
    public void setColor(float r, float g, float b) {
        _red = r;
        _green = g;
        _blue = b;
    }
    
    public void onDrawFrame(GL10 gl) {
    	super.onDrawFrame(gl);
        
        mShape.onDraw(gl);
//        /*
//         * Usually, the first thing one might want to do is to clear
//         * the screen. The most efficient way of doing this is to use
//         * glClear().
//         */
//		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//
//        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
////        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
//
//        mCamera.onDraw(gl);
    }
    
    public void onSurfaceChanged(GL10 gl, int width, int height) {
//    	mCamera.setScreenDimension(width, height).applyFrustrum(gl);
    	gl.glViewport(0, 0, width, height);
    }
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    	mShape.onCreate(gl);

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
//         gl.glEnable(GL10.GL_CULL_FACE);
//         gl.glShadeModel(GL10.GL_SMOOTH);
//         gl.glEnable(GL10.GL_DEPTH_TEST);
//         
//         cameraInit(gl);
    }
    
//    void cameraInit(GL10 gl) {
//    	mCamera.setLookAt(new Vector3f(mFigure.getLocation()));
//    	mCamera.setLocation(new Vector3f(mCamera.getLookAt()));
//    	mCamera.getLocation().add(0, 0, -mFigure.getLenZ()*4);
//    }
}
