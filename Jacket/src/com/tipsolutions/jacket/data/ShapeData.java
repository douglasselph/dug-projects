package com.tipsolutions.jacket.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class ShapeData {
	
	public static final int ELE_BOUNDS = 1;
	public static final int ELE_COLOR = 2;
	public static final int ELE_INDEX = 3;
	public static final int ELE_NORMAL = 4;
	public static final int ELE_VERTEX = 5;
	
	public static final int TYPE_FLOAT = 1;
	public static final int TYPE_SHORT = 2;
	
	protected ByteBuffer mColorBuf = null;
	protected ByteBuffer mIndexBuf = null;
    protected ByteBuffer mNormalBuf = null;
    protected ByteBuffer mVertexBuf = null;

	protected ShortData getColorData() { return null; }
	protected ShortData getIndexData() { return null; }
	protected FloatData getNormalData() { return null; }
	protected FloatData getVertexData() { return null; }
	
    protected static final int MIN_X = 0;
    protected static final int MIN_Y = 1;
    protected static final int MIN_Z = 2;
    protected static final int MAX_X = 3;
    protected static final int MAX_Y = 4;
    protected static final int MAX_Z = 5;
	protected float [] mBounds = null;
	// These values are computed in this class, and assumes
	// that readBounds() or computeBounds() has previously
	// been called.
	public float getMinX() { return mBounds[MIN_X]; }
	public float getMinY() { return mBounds[MIN_Y]; }
	public float getMinZ() { return mBounds[MIN_Z]; }
	public float getMaxX() { return mBounds[MAX_X]; }
	public float getMaxY() { return mBounds[MAX_Y]; }
	public float getMaxZ() { return mBounds[MAX_Z]; }
	
	// These values come from the blender file computation
	// (They are overridden in the super class):
	public float _getMaxX() { return 0; }
	public float _getMaxY() { return 0; }
	public float _getMaxZ() { return 0; }
	public float _getMinX() { return 0; }
	public float _getMinY() { return 0; }
	public float _getMinZ() { return 0; }
	
	public float getSizeX() { return getMaxX()-getMinX(); }
	public float getSizeY() { return getMaxY()-getMinY(); }
	public float getSizeZ() { return getMaxZ()-getMinZ(); }
	
	public interface FloatData {
		void fill(FloatBuffer buf);
		int size();
	}
	public interface MessageWriter {
		void msg(String msg);
	}
	public interface ShortData {
		void fill(ShortBuffer buf);
		int size();
	}
	static int compare(FloatBuffer buf1, FloatBuffer buf2, String who, MessageWriter msg) {
		int numDiffs = 0;
		if (buf1.limit() != buf2.limit()) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(who);
			sbuf.append(" buffers have different sizes: ");
			sbuf.append(buf1.limit());
			sbuf.append(" != ");
			sbuf.append(buf2.limit());
			msg.msg(sbuf.toString());
			numDiffs++;
		}
		buf1.rewind();
		buf2.rewind();
		float f1, f2;
		
		while (buf1.position() < buf1.limit()) {
			if ((f1 = buf1.get()) != (f2 = buf2.get())) {
    			StringBuffer sbuf = new StringBuffer();
    			sbuf.append(who);
    			sbuf.append(" different value at ");
    			sbuf.append(buf1.position());
    			sbuf.append(" :");
    			sbuf.append(f1);
    			sbuf.append(" != ");
    			sbuf.append(f2);
    			msg.msg(sbuf.toString());
    			numDiffs++;
			}
		}
		return numDiffs;
	}
	
	static boolean compare(MessageWriter msg, String name, float val1, float val2) {
		if (val1 != val2) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(name);
			sbuf.append(":");
			sbuf.append(val1);
			sbuf.append(" != ");
			sbuf.append(val2);
			msg.msg(sbuf.toString());
			return false;
		}
		return true;
    }
	
	static int compare(ShortBuffer buf1, ShortBuffer buf2, String who, MessageWriter msg) {
		int numDiffs = 0;
		if (buf1.limit() != buf2.limit()) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(who);
			sbuf.append(" buffers have different sizes: ");
			sbuf.append(buf1.limit());
			sbuf.append(" != ");
			sbuf.append(buf2.limit());
			msg.msg(sbuf.toString());
			numDiffs++;
		}
		buf1.rewind();
		buf2.rewind();
		short s1, s2;
		
		while (buf1.position() < buf1.limit()) {
			if ((s1 = buf1.get()) != (s2 = buf2.get())) {
    			StringBuffer sbuf = new StringBuffer();
    			sbuf.append(who);
    			sbuf.append(" different value at ");
    			sbuf.append(buf1.position());
    			sbuf.append(" :");
    			sbuf.append(s1);
    			sbuf.append(" != ");
    			sbuf.append(s2);
    			msg.msg(sbuf.toString());
    			numDiffs++;
			}
		}
		return numDiffs;
	}

	public void compare(ShapeData other, MessageWriter msg) {
		mVertexBuf.rewind();
		mNormalBuf.rewind();
		mIndexBuf.rewind();
		other.mVertexBuf.rewind();
		other.mNormalBuf.rewind();
		other.mIndexBuf.rewind();
		if (compare(mVertexBuf.asFloatBuffer(), other.mVertexBuf.asFloatBuffer(), "Vertex", msg) == 0) {
			msg.msg("Vertex buffer identical");
		}
		if (compare(mNormalBuf.asFloatBuffer(), other.mNormalBuf.asFloatBuffer(), "Normal", msg) == 0) {
			msg.msg("Normal buffer identical");
		}
		if (compare(mIndexBuf.asShortBuffer(), other.mIndexBuf.asShortBuffer(), "Index", msg) == 0) {
			msg.msg("Index buffer identical");
		}
		compare(msg, "MinX", getMinX(), other.getMinX());
		compare(msg, "MinY", getMinY(), other.getMinY());
		compare(msg, "MinZ", getMinZ(), other.getMinZ());
		compare(msg, "MaxX", getMaxX(), other.getMaxX());
		compare(msg, "MaxY", getMaxY(), other.getMaxY());
		compare(msg, "MaxZ", getMaxZ(), other.getMaxZ());
	}
	
	public void computeBounds() {
		FloatBuffer buf = mVertexBuf.asFloatBuffer();
		buf.rewind();
		
		mBounds = new float[6];
		float minX = buf.get();
		float minY = buf.get();
		float minZ = buf.get();
		float maxX = minX;
		float maxY = minY;
		float maxZ = minZ;
		
		float x, y, z;
		
		while (buf.position() < buf.limit()) {
			x = buf.get();
			y = buf.get();
			z = buf.get();
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
		mBounds[MIN_X] = minX;
		mBounds[MIN_Y] = minY;
		mBounds[MIN_Z] = minZ;
		mBounds[MAX_X] = maxX;
		mBounds[MAX_Y] = maxY;
		mBounds[MAX_Z] = maxZ;
	}
	
	public void draw(GL10 gl) {
		if (Constants.LOG) {
			Log.d(Constants.TAG, "draw():");
		}
		FloatBuffer fbuf;
		ShortBuffer sbuf;
		
		if (mVertexBuf != null) {
			fbuf = mVertexBuf.asFloatBuffer();
			fbuf.rewind(); gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fbuf);
			
			if (Constants.LOG) {
    			Log.d(Constants.TAG, "  " + toString("vertexbuf=", mVertexBuf.asFloatBuffer()));
			}
		}
		if (mNormalBuf != null) {
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuf.asFloatBuffer());
		}
		if (mColorBuf != null) {
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
		}
		if (mIndexBuf != null) {
			sbuf = mIndexBuf.asShortBuffer();
			sbuf.rewind();
			gl.glDrawElements(GL10.GL_TRIANGLES, sbuf.remaining(), GL10.GL_UNSIGNED_SHORT, sbuf);
			
			if (Constants.LOG) {
    			Log.d(Constants.TAG, "  " + toString("indexbuf=", mIndexBuf.asShortBuffer()));
			}
		}
	}
	
	public void fill() {
		mVertexBuf = fill(getVertexData());
		mNormalBuf = fill(getNormalData());
		mIndexBuf = fill(getIndexData());
		mColorBuf = fill(getColorData());
	}
	
	ByteBuffer fill(FloatData data) {
		if (data == null) {
			return null;
		}
		ByteBuffer vbb = ByteBuffer.allocateDirect(data.size()*4);
        vbb.order(ByteOrder.nativeOrder()); // Get this from android platform
        FloatBuffer buf = vbb.asFloatBuffer();
        data.fill(buf);
        return vbb;
	}
	
	ByteBuffer fill(ShortData data) {
		if (data == null) {
			return null;
		}
		ByteBuffer vbb = ByteBuffer.allocateDirect(data.size()*2);
        vbb.order(ByteOrder.nativeOrder()); // Get this from android platform
        ShortBuffer buf = vbb.asShortBuffer();
        data.fill(buf);
        return vbb;
	}
	
	protected void readBounds(DataInputStream dataStream) throws IOException {
		mBounds = new float[6];
		mBounds[MIN_X] = dataStream.readFloat();
		mBounds[MIN_Y] = dataStream.readFloat();
		mBounds[MIN_Z] = dataStream.readFloat();
		mBounds[MAX_X] = dataStream.readFloat();
		mBounds[MAX_Y] = dataStream.readFloat();
		mBounds[MAX_Z] = dataStream.readFloat();
	}
	
	protected ByteBuffer readBuffer(DataInputStream dataStream, int size) throws IOException {
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
        return vbb;
	};
	
	public void readData(InputStream inputStream) {
		try {
        	DataInputStream dataStream = new DataInputStream(inputStream);
        	int eleType;
        	int type;
        	int size;
        	ByteBuffer vbb;
        	
        	while (dataStream.available() > 0) {
        		eleType = dataStream.readInt();
        		
        		if (eleType == ELE_BOUNDS) {
        			readBounds(dataStream);
        		} else {
            		type = dataStream.readInt();
            		size = dataStream.readInt();
            		
            		vbb = readBuffer(dataStream, size);
            		
            		if (eleType == ELE_VERTEX) {
            			mVertexBuf = vbb;
            		} else if (eleType == ELE_NORMAL) {
            			mNormalBuf = vbb;
            		} else if (eleType == ELE_INDEX) {
            			mIndexBuf = vbb;
            		} else if (eleType == ELE_COLOR) {
            			mColorBuf = vbb;
            		}
        		}
        	}
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
	}
	
	public void readData(String filename) {
		File file = new File(filename);
		try {
			FileInputStream fileStream = new FileInputStream(file);
			readData(fileStream);
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
	}
	
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		if (mVertexBuf != null) {
			sbuf.append(toString("vertexbuf", mVertexBuf.asFloatBuffer()));
		}
		if (mIndexBuf != null) {
			sbuf.append(toString("indexbuf", mIndexBuf.asShortBuffer()));
		}
		return sbuf.toString();
	}
	
	public String toString(String name, FloatBuffer fbuf) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(name);
		sbuf.append("=");
		sbuf.append(fbuf.get());
		while (fbuf.hasRemaining()) {
			sbuf.append(",");
			sbuf.append(fbuf.get());
		}
		return sbuf.toString();
	}
	
	public String toString(String name, ShortBuffer fbuf) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(name);
		sbuf.append("=");
		sbuf.append(fbuf.get());
		while (fbuf.hasRemaining()) {
			sbuf.append(",");
			sbuf.append(fbuf.get());
		}
		return sbuf.toString();
	}
	
	protected void writeBounds(DataOutputStream dataStream) throws IOException {
		dataStream.writeInt(ELE_BOUNDS);
		dataStream.writeFloat(getMinX());
		dataStream.writeFloat(getMinY());
		dataStream.writeFloat(getMinZ());
		dataStream.writeFloat(getMaxX());
		dataStream.writeFloat(getMaxY());
		dataStream.writeFloat(getMaxZ());
	}
	
	protected void writeBuffer(DataOutputStream dataStream, int eleType, int dataType, ByteBuffer vbb) throws IOException {
		if (vbb != null) {
    		dataStream.writeInt(eleType);
    		dataStream.writeInt(dataType);
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
	
	public boolean writeData(String filename) {
		File file = new File(filename);
    	
    	try {
        	FileOutputStream fileStream = new FileOutputStream(file);
        	DataOutputStream dataStream = new DataOutputStream(fileStream);
        	
        	writeBounds(dataStream);
        	writeBuffer(dataStream, ELE_VERTEX, TYPE_FLOAT, mVertexBuf);
        	writeBuffer(dataStream, ELE_NORMAL, TYPE_FLOAT, mNormalBuf);
        	writeBuffer(dataStream, ELE_INDEX, TYPE_SHORT, mIndexBuf);
        	writeBuffer(dataStream, ELE_COLOR, TYPE_SHORT, mColorBuf);
        	
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    		return false;
    	}
    	return true;
	}
}
