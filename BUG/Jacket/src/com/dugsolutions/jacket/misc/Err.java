package com.dugsolutions.jacket.misc;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class Err
{
	static final String	TAG	= "Jacket.ERR";

	public static void printErrors(GL10 gl)
	{
		int err;
		while ((err = gl.glGetError()) != GL10.GL_NO_ERROR)
		{
			printError(getError(err));
		}
	}

	static void printError(String msg)
	{
		Log.e(TAG, msg);
	}

	static String getError(int code)
	{
		switch (code)
		{
			case GL10.GL_INVALID_ENUM:
				return "Invalid enum";
			case GL10.GL_INVALID_OPERATION:
				return "Invalid operation";
			case GL10.GL_INVALID_VALUE:
				return "Invalid value";
			case GL10.GL_STACK_OVERFLOW:
				return "Stack overflow";
			case GL10.GL_STACK_UNDERFLOW:
				return "Stack underflow";
			case GL10.GL_OUT_OF_MEMORY:
				return "out of memory";
		}
		return null;
	}
}
