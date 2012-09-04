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

package com.dugsolutions.testcube;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.math.Vector3f;

/**
 * A vertex shaded cube.
 */
public class Cube
{
	static final int	vOne	= 0x07000;
	static final int	cOne	= 0x10000;

	private IntBuffer	mVertexBuffer;
	private IntBuffer	mColorBuffer;
	private ByteBuffer	mIndexBuffer;

	public Cube()
	{
		final int vertices[] = {
				-vOne, -vOne, -vOne, vOne, -vOne, -vOne, vOne, vOne, -vOne, -vOne, vOne, -vOne, -vOne, -vOne, vOne,
				vOne, -vOne, vOne, vOne, vOne, vOne, -vOne, vOne, vOne, };

		final int colors[] = {
				0, 0, 0, cOne, cOne, 0, 0, cOne, cOne, cOne, 0, cOne, 0, cOne, 0, cOne, 0, 0, cOne, cOne, cOne, 0,
				cOne, cOne, cOne, cOne, cOne, cOne, 0, cOne, cOne, cOne, };

		final byte indices[] = {
				0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7, 3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3,
				1, 2 };
		// Buffers to be passed to gl*Pointer() functions
		// must be direct, i.e., they must be placed on the
		// native heap where the garbage collector cannot
		// move them.
		//
		// Buffers with multi-byte datatypes (e.g., short, int, float)
		// must have their byte order set to native order

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
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

	public void draw(GL10 gl)
	{
		gl.glFrontFace(GL10.GL_CW);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
		gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
	}

	public Vector3f getLoc()
	{
		return new Vector3f(0, 0, 0);
	};
}
