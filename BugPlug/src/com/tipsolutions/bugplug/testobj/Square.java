package com.tipsolutions.bugplug.testobj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

public class Square {

	final float			mVertices[]	= { -1f, -1f, 0f, // 0
			-1f, 1f, 0f, // 1
			1f, -1f, 0f, // 2
			1f, 1f, 0f				// 3
									};
	final float			mTexPts[]	= { 0f, 1f, // top left
			0f, 0f, // bottom left
			1f, 1f, // top right
			1f, 0f					// bottom right
									};

	private FloatBuffer	mVertexBuffer;
	private FloatBuffer	mTextureBuffer;
	private int			mTextureID;

	public Square() {
		// a float has 4 bytes so we allocate for each coordinate 4 bytes
		ByteBuffer vertexByteBuffer = ByteBuffer
				.allocateDirect(mVertices.length * 4);
		vertexByteBuffer.order(ByteOrder.nativeOrder());
		// allocates the memory from the byte buffer
		mVertexBuffer = vertexByteBuffer.asFloatBuffer();
		// fill the vertexBuffer with the vertices
		mVertexBuffer.put(mVertices);
		// set the cursor position to the beginning of the buffer
		mVertexBuffer.position(0);

		ByteBuffer textureByteBuffer = ByteBuffer
				.allocateDirect(mTexPts.length * 4);
		textureByteBuffer.order(ByteOrder.nativeOrder());
		mTextureBuffer = textureByteBuffer.asFloatBuffer();
		mTextureBuffer.put(mTexPts);
		mTextureBuffer.position(0);
	}

	public void draw(GL10 gl, boolean doTexture) {
		gl.glFrontFace(GL10.GL_CW);

		if (doTexture)
		{
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
		} else
		{
			gl.glColor4f(0f, 1f, 0f, 0.5f);
		}
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	public void loadTexture(GL10 gl, Context context, int resId) {
		int textures[] = new int[1];

		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
				resId);
		// generate one texture pointer
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		// Use Android GLUtils to specify a two-dimensional texture image from
		// our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		// Clean up
		bitmap.recycle();

		mTextureID = textures[0];
	}
}
