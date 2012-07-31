/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tipsolutions.bugplug.testobj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.tipsolutions.jacket.math.Vector3f;

/**
 * A vertex shaded cube.
 */
public class CubeF {
	static final float	vOne	= 0.7f;
	static final int	cOne	= 0x10000;

	public CubeF() {
		final float vertices[] = { -vOne, -vOne, -vOne, // 0
				vOne, -vOne, -vOne, // 1
				vOne, vOne, -vOne, // 2
				-vOne, vOne, -vOne, // 3
				-vOne, -vOne, vOne, // 4
				vOne, -vOne, vOne, // 5
				vOne, vOne, vOne, // 6
				-vOne, vOne, vOne, }; // 7

		final int colors[] = { 0, 0, 0, cOne, // 0
				cOne, 0, 0, cOne, // 1
				cOne, cOne, 0, cOne, // 2
				0, cOne, 0, cOne, // 3
				0, 0, cOne, cOne, // 4
				cOne, 0, cOne, cOne, // 5
				cOne, cOne, cOne, cOne, // 6
				0, cOne, cOne, cOne, }; // 7

		final byte indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7,
				2, 7, 3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };
		// Buffers to be passed to gl*Pointer() functions
		// must be direct, i.e., they must be placed on the
		// native heap where the garbage collector cannot
		// move them.
		//
		// Buffers with multi-byte datatypes (e.g., short, int, float)
		// must have their byte order set to native order

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asIntBuffer();
		mColorBuffer.put(colors);
		mColorBuffer.position(0);

		mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
		mIndexBuffer.put(indices);
		mIndexBuffer.position(0);
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE,
				mIndexBuffer);
	}

	private FloatBuffer	mVertexBuffer;
	private IntBuffer	mColorBuffer;
	private ByteBuffer	mIndexBuffer;

	public Vector3f getLoc() {
		return new Vector3f(0, 0, 0);
	};
}
