package com.tipsolutions.slice;

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

public class FigureData {

	public static final int ELE_INDEX = 3;
	public static final int ELE_NORMAL = 2;
	
	public static final int ELE_VERTEX = 1;
	public static final int TYPE_FLOAT = 1;
	public static final int TYPE_SHORT = 2;
	
	ByteBuffer mNormalBuf = null;
	ByteBuffer mVertexBuf = null;
	ByteBuffer mIndexBuf = null;
	ByteBuffer mColorBuffer = null;
	int mIndexCount = 0;
	
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

	public void compare(FigureData other, MessageWriter msg) {
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
	}
	
	public void fill() {
		mVertexBuf = fill(getVertexData());
		mNormalBuf = fill(getNormalData());
		mIndexBuf = fill(getIndexData());
	}
	
	ByteBuffer fill(FloatData data) {
		ByteBuffer vbb = ByteBuffer.allocateDirect(data.size()*4);
        vbb.order(ByteOrder.nativeOrder()); // Get this from android platform
        FloatBuffer buf = vbb.asFloatBuffer();
        data.fill(buf);
        return vbb;
	}
	
	ByteBuffer fill(ShortData data) {
		ByteBuffer vbb = ByteBuffer.allocateDirect(data.size()*2);
        vbb.order(ByteOrder.nativeOrder()); // Get this from android platform
        ShortBuffer buf = vbb.asShortBuffer();
        data.fill(buf);
        return vbb;
	}
	
	public float getMinX() { return 0; }
	public float getMinY() { return 0; }
	public float getMinZ() { return 0; }
	public float getMaxX() { return 0; }
	public float getMaxY() { return 0; }
	public float getMaxZ() { return 0; }
	
	ShortData getIndexData() { return null; }
	FloatData getNormalData() { return null; }
	FloatData getVertexData() { return null; }
	
	ByteBuffer readBuffer(DataInputStream dataStream, int size) throws IOException {
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
	}
	
	public void readData(InputStream inputStream) {
		try {
        	DataInputStream dataStream = new DataInputStream(inputStream);
        	int eleType;
        	int type;
        	int size;
        	ByteBuffer vbb;
        	
        	while (dataStream.available() > 0) {
        		eleType = dataStream.readInt();
        		type = dataStream.readInt();
        		size = dataStream.readInt();
        		
        		vbb = readBuffer(dataStream, size);
        		
        		if (eleType == ELE_VERTEX) {
        			mVertexBuf = vbb;
        		} else if (eleType == ELE_NORMAL) {
        			mNormalBuf = vbb;
        		} else if (eleType == ELE_INDEX) {
        			mIndexBuf = vbb;
        		}
        	}
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
	};
	
	public void readData(String filename) {
		File file = new File(filename);
		try {
			FileInputStream fileStream = new FileInputStream(file);
			readData(fileStream);
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
	}
	
	void writeBuffer(DataOutputStream dataStream, int eleType, int dataType, ByteBuffer vbb) throws IOException {
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
	
	public boolean writeData(String filename) {
		File file = new File(filename);
    	
    	try {
        	FileOutputStream fileStream = new FileOutputStream(file);
        	DataOutputStream dataStream = new DataOutputStream(fileStream);
        	
        	writeBuffer(dataStream, ELE_VERTEX, TYPE_FLOAT, mVertexBuf);
        	writeBuffer(dataStream, ELE_NORMAL, TYPE_FLOAT, mNormalBuf);
        	writeBuffer(dataStream, ELE_INDEX, TYPE_SHORT, mIndexBuf);
        	
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    		return false;
    	}
    	return true;
	}
}
