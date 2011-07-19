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

public class ShapeData {
	
	public static final int ELE_BOUNDS = 1;
	public static final int ELE_COLOR = 2;
	public static final int ELE_INDEX = 3;
	public static final int ELE_NORMAL = 4;
	public static final int ELE_VERTEX = 5;
	public static final int ELE_TEXTURE = 6;
	
	public static final int TYPE_FLOAT = 1;
	public static final int TYPE_SHORT = 2;
	
	class FloatBuf {
		ByteBuffer mRoot;
		FloatBuffer mBuf;
		
		void set(FloatData data) {
			if (data == null) {
				mRoot = null;
				mBuf = null;
			} else {
    			mRoot = ByteBuffer.allocateDirect(data.size()*4);
    			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
    			mBuf = mRoot.asFloatBuffer();
    			data.fill(mBuf);
			}
		}
		
		void set(ByteBuffer buf) {
			mRoot = buf;
			mBuf = buf.asFloatBuffer();
		}
		
		FloatBuffer set(int size) {
			mRoot = ByteBuffer.allocateDirect(size*4);
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
			return mBuf = mRoot.asFloatBuffer();
		}
		
		FloatBuffer getBuf() {
			if (mBuf != null) {
				mBuf.rewind();
			}
			return mBuf;
		}
		
		ByteBuffer getRootBuf() {
			if (mRoot != null) {
				mRoot.rewind();
			}
			return mRoot;
		}
		
		boolean hasData() {
			return (mRoot != null);
		}
	};
	
	class ShortBuf {
		ByteBuffer mRoot;
		ShortBuffer mBuf;
		
		void set(ShortData data) {
			if (data == null) {
				mRoot = null;
				mBuf = null;
			}
			mRoot = ByteBuffer.allocateDirect(data.size()*2);
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
			mBuf = mRoot.asShortBuffer();
			data.fill(mBuf);
		}
		
		void set(ByteBuffer buf) {
			mRoot = buf;
			mBuf = buf.asShortBuffer();
		}
		
		ShortBuffer set(int size) {
			mRoot = ByteBuffer.allocateDirect(size*2);
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
			return mBuf = mRoot.asShortBuffer();
		}
		
		ShortBuffer getBuf() {
			if (mBuf != null) {
    			mBuf.rewind();
			}
			return mBuf;
		}
		
		ByteBuffer getRootBuf() {
			if (mRoot != null) {
    			mRoot.rewind();
			}
			return mRoot;
		}
	};
	
	protected FloatBuf mColorBuf = new FloatBuf();
	protected ShortBuf mIndexBuf = new ShortBuf();
    protected FloatBuf mNormalBuf = new FloatBuf();
    protected FloatBuf mVertexBuf = new FloatBuf();
    protected FloatBuf mTextureBuf = new FloatBuf();
    
	protected int mIndexMode = GL10.GL_TRIANGLES;
	
//	static final protected int FIXED_COLOR_ONE = 0x10000;
//	protected ShortData getColorFixed() { return null; }

	protected FloatData getColorData() { return null; }
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

	// Public access uses computed bounds if done, otherwise uses super class define
	public float getMaxX() { return mBounds == null ? _getMaxX() : _getMaxXb(); }
	public float getMaxY() { return mBounds == null ? _getMaxY() : _getMaxYb(); }
	public float getMaxZ() { return mBounds == null ? _getMaxZ() : _getMaxZb(); }
	public float getMinX() { return mBounds == null ? _getMinX() : _getMinXb(); }
	public float getMinY() { return mBounds == null ? _getMinY() : _getMinYb(); }
	public float getMinZ() { return mBounds == null ? _getMinZ() : _getMinZb(); }
	
	// These values are computed in this class, and assumes
	// that readBounds() or computeBounds() has previously
	// been called.
	protected float _getMinXb() { return mBounds[MIN_X]; }
	protected float _getMinYb() { return mBounds[MIN_Y]; }
	protected float _getMinZb() { return mBounds[MIN_Z]; }
	protected float _getMaxXb() { return mBounds[MAX_X]; }
	protected float _getMaxYb() { return mBounds[MAX_Y]; }
	protected float _getMaxZb() { return mBounds[MAX_Z]; }
	
	// These values come from the blender file computation
	// (They are overridden in the super class):
	protected float _getMaxX() { return 0; }
	protected float _getMaxY() { return 0; }
	protected float _getMaxZ() { return 0; }
	protected float _getMinX() { return 0; }
	protected float _getMinY() { return 0; }
	protected float _getMinZ() { return 0; }
	
	protected void setMaxX(float x) { mBounds[MAX_X] = x; }
	protected void setMaxY(float y) { mBounds[MAX_Y] = y; }
	protected void setMaxZ(float z) { mBounds[MAX_Z] = z; }
	protected void setMinX(float x) { mBounds[MIN_X] = x; }
	protected void setMinY(float y) { mBounds[MIN_Y] = y; }
	protected void setMinZ(float z) { mBounds[MIN_Z] = z; }
	
	public float getSizeX() { return getMaxX()-getMinX(); }
	public float getSizeY() { return getMaxY()-getMinY(); }
	public float getSizeZ() { return getMaxZ()-getMinZ(); }
	
	public FloatBuffer getColorBuf() { return mVertexBuf.getBuf(); }
	public ShortBuffer getIndexBuf() { return mIndexBuf.getBuf(); }
	public FloatBuffer getVertexBuf() { return mVertexBuf.getBuf(); }
	public FloatBuffer getNormalBuf() { return mNormalBuf.getBuf(); }
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }
	
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
		if (buf1 == null && buf2 == null) {
			return 0;
		}
		if (buf1 == null) {
			msg.msg("First buffer was null");
		} else if (buf2 == null) {
			msg.msg("Second buffer was null");
		}
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
		FloatBuffer vertexBuf = getVertexBuf();
		FloatBuffer normalBuf = getNormalBuf();
		ShortBuffer indexBuf = getIndexBuf();
		FloatBuffer colorBuf = getColorBuf();
		
		FloatBuffer vertexBufO = other.getVertexBuf();
		FloatBuffer normalBufO = other.getNormalBuf();
		ShortBuffer indexBufO = other.getIndexBuf();
		FloatBuffer colorBufO = other.getColorBuf();
		
		if (compare(vertexBuf, vertexBufO, "Vertex", msg) == 0) {
			msg.msg("Vertex buffer identical");
		}
		if (compare(normalBuf, normalBufO, "Normal", msg) == 0) {
			msg.msg("Normal buffer identical");
		}
		if (compare(indexBuf, indexBufO, "Index", msg) == 0) {
			msg.msg("Index buffer identical");
		}
		if (compare(colorBuf, colorBufO, "Color", msg) == 0) {
			msg.msg("Color buffer identical");
		}
		compare(msg, "MinX", getMinX(), other.getMinX());
		compare(msg, "MinY", getMinY(), other.getMinY());
		compare(msg, "MinZ", getMinZ(), other.getMinZ());
		compare(msg, "MaxX", getMaxX(), other.getMaxX());
		compare(msg, "MaxY", getMaxY(), other.getMaxY());
		compare(msg, "MaxZ", getMaxZ(), other.getMaxZ());
	}
	
	protected void allocBounds() {
		if (mBounds == null) {
    		mBounds = new float[6];
		}
	}
	
	public void computeBounds() {
		FloatBuffer buf = getVertexBuf();
		
		allocBounds();
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
		setMinX(minX);
		setMinY(minY);
		setMinZ(minZ);
		setMaxX(maxX);
		setMaxY(maxY);
		setMaxZ(maxZ);
	}

	public void fill() {
		setVertexData(getVertexData());
		setNormalData(getNormalData());
		setIndexData(getIndexData());
		setColorData(getColorData());
		
		// It is arguably faster and easier to use floats
		// because modern hardware supports colors floats.
		// If not, then it is faster to use GL_FIXED.
		// Right now, this code is optimized for modern hardware.
		
//		setColorData(getColorFixed()); // Note: uses FIXED, which means one is 0x10000.
	}
	
	public boolean hasVertexArray() {
		return (mVertexBuf.getBuf() != null);
	}
	
	public boolean hasNormalArray() {
		return (mNormalBuf.getBuf() != null);
	}
	
	public boolean hasColorArray() {
		return (mColorBuf.getBuf() != null);
	}
	
	public boolean hasTextureArray() {
		return (mTextureBuf.getBuf() != null);
	}
	
	public void onDraw(GL10 gl) {
		FloatBuffer fbuf;
		if ((fbuf = getVertexBuf()) != null) {
    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fbuf);
		}
		if ((fbuf = getNormalBuf()) != null) {
    		gl.glNormalPointer(GL10.GL_FLOAT, 0, fbuf);
		}
		if ((fbuf = getColorBuf()) != null) {
    		gl.glColorPointer(4, GL10.GL_FLOAT, 0, fbuf);
    		
    		// Not doing it this way anymore:
//    		gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
		}
		if (mIndexBuf != null) {
			ShortBuffer sbuf = getIndexBuf();
			gl.glDrawElements(mIndexMode, sbuf.remaining(), GL10.GL_UNSIGNED_SHORT, sbuf);
		}
	}
	
	protected void readBounds(DataInputStream dataStream) throws IOException {
		allocBounds();
		setMinX(dataStream.readFloat());
		setMinY(dataStream.readFloat());
		setMinZ(dataStream.readFloat());
		setMaxX(dataStream.readFloat());
		setMaxY(dataStream.readFloat());
		setMaxZ(dataStream.readFloat());
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
	
	public void readData(InputStream inputStream) throws Exception, IOException {
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
					if (type != TYPE_FLOAT) {
						throw new Exception("Expected float type for vertex");
					}
					mVertexBuf.set(vbb);
				} else if (eleType == ELE_NORMAL) {
					if (type != TYPE_FLOAT) {
						throw new Exception("Expected float type for normals");
					}
					mNormalBuf.set(vbb);
				} else if (eleType == ELE_INDEX) {
					if (type != TYPE_SHORT) {
						throw new Exception("Expected short type for indexes");
					}
					mIndexBuf.set(vbb);
				} else if (eleType == ELE_COLOR) {
					if (type != TYPE_FLOAT) {
						throw new Exception("Expected float type for colors");
					}
					mColorBuf.set(vbb);
				}
			}
		}
		dataStream.close();
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
	
//	public void setColorData(ShortData data) {
//		if (data != null) {
//    		mColorBuf = fill(data);
//		}
//	}
	
	public void setColorData(FloatData data) {
		if (data != null) {
			mColorBuf = new FloatBuf();
    		mColorBuf.set(data);
		}
	}
	
	public void setIndexData(ShortData data) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
	}
	
	public void setIndexData(ShortData data, int mode) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
		mIndexMode = mode;
	}
	
	public void setNormalData(FloatData data) {
		mNormalBuf = new FloatBuf();
		mNormalBuf.set(data);
	}
	
	public void setVertexData(FloatData data) {
		mVertexBuf = new FloatBuf();
		mVertexBuf.set(data);
	}
	
	public FloatBuffer setColorBuf(int size) {
		mColorBuf = new FloatBuf();
		return mColorBuf.set(size);
	}
	
	public ShortBuffer setIndexBuf(int size) {
		mIndexBuf = new ShortBuf();
		return mIndexBuf.set(size);
	}
	
	public FloatBuffer setNormalBuf(int size) {
		mNormalBuf = new FloatBuf();
		return mNormalBuf.set(size);
	}
	
	public FloatBuffer setVertexBuf(int size) {
		mVertexBuf = new FloatBuf();
		return mVertexBuf.set(size);
	}
	
	public FloatBuffer setTextureBuf(int size) {
		mTextureBuf = new FloatBuf();
		return mTextureBuf.set(size);
	}
		
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		if (mVertexBuf != null) {
			sbuf.append(toString("vertexbuf", mVertexBuf.getBuf()));
		}
		if (mIndexBuf != null) {
			sbuf.append(toString("indexbuf", mIndexBuf.getBuf()));
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
        	writeBuffer(dataStream, ELE_VERTEX, TYPE_FLOAT, mVertexBuf.getRootBuf());
        	writeBuffer(dataStream, ELE_NORMAL, TYPE_FLOAT, mNormalBuf.getRootBuf());
        	writeBuffer(dataStream, ELE_INDEX, TYPE_SHORT, mIndexBuf.getRootBuf());
        	writeBuffer(dataStream, ELE_COLOR, TYPE_FLOAT, mColorBuf.getRootBuf());
        	
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    		return false;
    	}
    	return true;
	}
}
