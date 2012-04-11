package com.tipsolutions.jacket.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BufUtils {
	
	static public class Bounds {
		protected static final int MIN_X = 0;
		protected static final int MIN_Y = 1;
		protected static final int MIN_Z = 2;
		protected static final int MAX_X = 3;
		protected static final int MAX_Y = 4;
		protected static final int MAX_Z = 5;
		
		protected static final int SIZ = 6;

		float [] mBounds = new float[SIZ];
		
		public float getMinX() { return mBounds[MIN_X]; }
		public float getMinY() { return mBounds[MIN_Y]; }
		public float getMinZ() { return mBounds[MIN_Z]; }
		public float getMaxX() { return mBounds[MAX_X]; }
		public float getMaxY() { return mBounds[MAX_Y]; }
		public float getMaxZ() { return mBounds[MAX_Z]; }
		
		public void setMaxX(float x) { mBounds[MAX_X] = x; }
		public void setMaxY(float y) { mBounds[MAX_Y] = y; }
		public void setMaxZ(float z) { mBounds[MAX_Z] = z; }
		public void setMinX(float x) { mBounds[MIN_X] = x; }
		public void setMinY(float y) { mBounds[MIN_Y] = y; }
		public void setMinZ(float z) { mBounds[MIN_Z] = z; }
		
		public float getSizeX() { return getMaxX()-getMinX(); }
		public float getSizeY() { return getMaxY()-getMinY(); }
		public float getSizeZ() { return getMaxZ()-getMinZ(); }
		
		public float getMidX() { return (getMaxX()+getMinX())/2; }
		public float getMidY() { return (getMaxY()+getMinY())/2; }
		public float getMidZ() { return (getMaxZ()+getMinZ())/2; }
		
		public void set(ComputeBounds computeBounds) {
			mBounds = new float[SIZ];
			
			setMinX(computeBounds.minX);
			setMinY(computeBounds.minY);
			setMinZ(computeBounds.minZ);
			setMaxX(computeBounds.maxX);
			setMaxY(computeBounds.maxY);
			setMaxZ(computeBounds.maxZ);
		}
		
		public void write(DataOutputStream dataStream) throws IOException {
			dataStream.writeFloat(getMinX());
			dataStream.writeFloat(getMinY());
			dataStream.writeFloat(getMinZ());
			dataStream.writeFloat(getMaxX());
			dataStream.writeFloat(getMaxY());
			dataStream.writeFloat(getMaxZ());
		}
		
		public void read(DataInputStream dataStream) throws IOException {
			setMinX(dataStream.readFloat());
			setMinY(dataStream.readFloat());
			setMinZ(dataStream.readFloat());
			setMaxX(dataStream.readFloat());
			setMaxY(dataStream.readFloat());
			setMaxZ(dataStream.readFloat());
		}
		
		public boolean within(float x, float y, float z) {
			return (x >= getMinX() && x <= getMaxX() &&
				    y >= getMinY() && y <= getMaxY() &&
				    z >= getMinZ() && z <= getMaxZ());
		}
		
		public boolean within(float x, float y) {
			return (x >= getMinX() && x <= getMaxX() &&
				    y >= getMinY() && y <= getMaxY());
		}
		
		@Override
		public String toString() {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("Bounds[");
			sbuf.append(getMinX());
			sbuf.append("->");
			sbuf.append(getMaxX());
			sbuf.append(",");
			sbuf.append(getMinY());
			sbuf.append("->");
			sbuf.append(getMaxY());
			sbuf.append(",");
			sbuf.append(getMinZ());
			sbuf.append("->");
			sbuf.append(getMaxZ());
			sbuf.append("]");
			return sbuf.toString();
		}
	};
	
	static public class ComputeBounds {
		public float minX = 0;
		public float minY = 0;
		public float minZ = 0;
		public float maxX = 0;
		public float maxY = 0;
		public float maxZ = 0;
		protected boolean initialized = false;

		public ComputeBounds() {}

		public void apply(float x, float y, float z) {
			if (!initialized) {
				minX = x;
				minY = y;
				minZ = z;
				maxX = x;
				maxY = y;
				maxZ = z;
				initialized = true;
			} else {
				if (x < minX) {
					minX = x;
				} else if (x > maxX) {
					maxX = x;
				}
				if (y < minY) {
					minY = y;
				} else if (y > maxY) {
					maxY = y;
				}
				if (z < minZ) {
					minZ = z;
				} else if (z > maxZ) {
					maxZ = z;
				}
			}
		}
	};
	
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
