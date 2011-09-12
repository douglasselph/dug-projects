package com.tipsolutions.jacket.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.data.Shape.dData;

public class BufferUtils {
    /**
     * Copies a Vector3 from one position in the buffer to another. The index values are in terms of vector number (eg,
     * vector number 0 is positions 0-2 in the FloatBuffer.)
     * 
     * @param buf
     *            the buffer to copy from/to
     * @param fromPos
     *            the index of the vector to copy
     * @param toPos
     *            the index to copy the vector to
     */
    public static void copyInternalVector3(final FloatBuffer buf, final int fromPos, final int toPos) {
        copyInternal(buf, fromPos * 3, toPos * 3, 3);
    }
    
    /**
     * Copies floats from one position in the buffer to another.
     * 
     * @param buf
     *            the buffer to copy from/to
     * @param fromPos
     *            the starting point to copy from
     * @param toPos
     *            the starting point to copy to
     * @param length
     *            the number of floats to copy
     */
    public static void copyInternal(final FloatBuffer buf, final int fromPos, final int toPos, final int length) {
        final float[] data = new float[length];
        buf.position(fromPos);
        buf.get(data);
        buf.position(toPos);
        buf.put(data);
    }
    
    /**
     * Updates the values of the given vector from the specified buffer at the index provided.
     * 
     * @param vector
     *            the vector to set data on
     * @param buf
     *            the buffer to read from
     * @param index
     *            the position (in terms of vectors, not floats) to read from the buf
     */
    public static void populateFromBuffer(final Vector3 vector, final FloatBuffer buf, final int index) {
        vector.setX(buf.get(index * 3));
        vector.setY(buf.get(index * 3 + 1));
        vector.setZ(buf.get(index * 3 + 2));
    }
    
    // 
    // BUFFERS
    //
	public static final int TYPE_FLOAT = 1;
	public static final int TYPE_SHORT = 2;
	
	public static abstract class Buffer<BUFTYPE> {
		ByteBuffer mRoot;
		BUFTYPE mBuf;
		
		public void set(dData<BUFTYPE> data) {
			if (data == null) {
				mRoot = null;
				mBuf = null;
			} else {
    			mRoot = ByteBuffer.allocateDirect(data.size()*getSize());
    			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
    			mBuf = asBuffer(mRoot);
    			data.fill(mBuf);
			}
		}
		
		public abstract BUFTYPE asBuffer(ByteBuffer buf);
		public abstract void rewind(BUFTYPE buf);
		public abstract int capacity(BUFTYPE buf);
		abstract int getSize();
		abstract int getType();
		
		void set(ByteBuffer buf) {
			mRoot = buf;
			mBuf = asBuffer(buf);
		}
		
		public BUFTYPE alloc(int size) {
			mRoot = ByteBuffer.allocateDirect(size*getSize());
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
			mBuf = asBuffer(mRoot);
			return mBuf;
		}
		
		public BUFTYPE getBuf() {
			if (mBuf != null) {
				rewind(mBuf);
			}
			return mBuf;
		}
		
		public ByteBuffer getRootBuf() {
			if (mRoot != null) {
				mRoot.rewind();
			}
			return mRoot;
		}
		
		public boolean hasData() {
			return (mRoot != null);
		}
		
		public int capacity() {
			if (mBuf == null) {
				return 0;
			}
			return capacity(mBuf);
		}
		
		public void writeBuffer(DataOutputStream dataStream) throws IOException {
			ByteBuffer vbb = getRootBuf();
			if (vbb != null) {
	    		dataStream.writeInt(getType());
	    		dataStream.writeInt(vbb.limit());
	    		if (vbb.hasArray()) {
	        		dataStream.write(vbb.array(), 0, vbb.limit());
	    		} else {
	    			vbb.rewind();
	    			byte [] dst = new byte[vbb.limit()];
	    			vbb.get(dst);
	        		dataStream.write(dst, 0, vbb.limit());
	        		vbb.rewind();
	    		}
			}
		}
		
		public void readBuffer(DataInputStream dataStream) throws IOException, Exception {
			int type = dataStream.readInt();
			int size = dataStream.readInt();
			
			if (type != getType()) {
				if (getType() == TYPE_FLOAT) {
					throw new Exception("Expected float type");
				}
				if (getType() == TYPE_SHORT) {
					throw new Exception("Expected short type");
				}
				throw new Exception("Expected a different type");
			}
			ByteBuffer vbb = ByteBuffer.allocateDirect(size);
	        vbb.order(ByteOrder.nativeOrder()); // Get this from android platform
	        if (vbb.hasArray()) {
	            dataStream.read(vbb.array(), 0, size);
	        } else {
	        	byte [] dst = new byte[size];
	            dataStream.read(dst, 0, size);
	            vbb.rewind();
	            vbb.put(dst);
	            vbb.rewind();
	        }
	        set(vbb);
		}
	};

	public static class FloatBuf extends Buffer<FloatBuffer> {
		@Override public FloatBuffer asBuffer(ByteBuffer buf) { return buf.asFloatBuffer(); } 
		@Override public int capacity(FloatBuffer buf) { return buf.capacity(); }
		@Override public void rewind(FloatBuffer buf) { buf.rewind(); }
		@Override int getSize() { return 4; }
		@Override int getType() { return TYPE_FLOAT; }
	};
	
	public static class ShortBuf extends Buffer<ShortBuffer> {
		@Override public ShortBuffer asBuffer(ByteBuffer buf) { return buf.asShortBuffer(); }
		@Override public int capacity(ShortBuffer buf) { return buf.capacity(); }
		@Override public void rewind(ShortBuffer buf) { buf.rewind(); }
		@Override int getSize() { return 2; }
		@Override int getType() { return TYPE_SHORT; }
	};
	
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

	
}
