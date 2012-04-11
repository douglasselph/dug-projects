package com.tipsolutions.jacket.shape;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.Vector3;

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
	public interface dData<BUFTYPE> {
		void fill(BUFTYPE buf);
		int size();
	};
	
	public interface dFloatBuf extends dData<FloatBuffer> {
	};
	
	public interface dShortBuf extends dData<ShortBuffer> {
	};
	
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
		
		public void rewind() { rewind(getBuf()); }
		public boolean hasRemaining() { return mBuf.hasRemaining(); }
		public float get() { return mBuf.get(); }
		public int limit() { return mBuf.limit(); }
	};
	
	public static class ShortBuf extends Buffer<ShortBuffer> {
		@Override public ShortBuffer asBuffer(ByteBuffer buf) { return buf.asShortBuffer(); }
		@Override public int capacity(ShortBuffer buf) { return buf.capacity(); }
		@Override public void rewind(ShortBuffer buf) { buf.rewind(); }
		@Override int getSize() { return 2; }
		@Override int getType() { return TYPE_SHORT; }
		
		public void rewind() { rewind(getBuf()); }
		public boolean hasRemaining() { return mBuf.hasRemaining(); }
		public short get() { return mBuf.get(); }
		public int limit() { return mBuf.limit(); }
	};
	
	// If N+1 > N then two disconnected values.
	// If N+1 < N then treat as range of values between N+1 .. N
	public static class ShortBufSortedRange extends ShortBuf {
		int mLimit = 0;
		int mPos = 0;
		
		// To Support get() routine:
		boolean mInRange = false;
		boolean mHasPrevious = false;
		short mPrevious = 0;
		short mCurValue = 0;
		short mEndValue = 0;
		
		@Override 
		public void rewind() {
			super.rewind();
			mPos = 0;
			mHasPrevious = false;
			mInRange = false;
		}
		
		@Override
		public int limit() {
			if (mLimit == 0) {
				super.rewind();
				boolean hasPrevious = false;
				short value;
				short previous = 0;
				
				while (super.hasRemaining()) {
					value = super.get();
					if (!hasPrevious) {
						previous = value;
						hasPrevious = true;
					} else if (value < previous) {
						mLimit += (previous-value+1);
						hasPrevious = false;
					} else {
						mLimit++;
						previous = value;
					}
				}
				if (hasPrevious) {
					mLimit++;
				}
				super.rewind();
			}
			return mLimit;
		}
		
		@Override 
		public boolean hasRemaining() {
			return (mPos < limit());
		}
		
		@Override 
		public short get() {
			if (!hasRemaining()) {
				return 0;
			}
			short value;
			
			if (mInRange) {
				if (++mCurValue <= mEndValue) {
					mPos++;
					return mCurValue;
				}
				mInRange = false;
			}
			if (!super.hasRemaining()) {
				if (mHasPrevious) {
					mPos++;
					return mPrevious;
				}
				return 0; // should never get here
			}
			value = super.get();
			
			if (!mHasPrevious) {
				if (super.hasRemaining()) {
					mPrevious = value;
					mHasPrevious = true;
					value = super.get();
				} else {
					mPos++;
					return value;
				}
			}
			if (value < mPrevious) {
				mInRange = true;
				mCurValue = value;
				mEndValue = mPrevious;
				mHasPrevious = false;
			} else {
				mCurValue = mPrevious;
				mPrevious = value;
			}
			mPos++;
			return mCurValue;
		}
	};
	


	
}
