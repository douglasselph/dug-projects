package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BufUtils {
	
	static public FloatBuffer setSize(FloatBuffer buf, int size) {
		if (buf == null || buf.capacity() < size) {
			return FloatBuffer.allocate(size);
		}
		buf.limit(size);
		return buf;
	}
	
	static public ShortBuffer setSize(ShortBuffer buf, int size) {
		if (buf == null || buf.capacity() < size) {
			return ShortBuffer.allocate(size);
		}
		buf.limit(size);
		return buf;
	}
}
