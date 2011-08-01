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

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;

public class ShapeData {
	
	protected static final int FILE_VERSION = 1;
	
	protected static final int ELE_FINISH 		   = "finish".hashCode();
	protected static final int ELE_VERSION 		   = "version".hashCode();
	protected static final int ELE_BOUNDS 		   = "bounds".hashCode();
	protected static final int ELE_COLOR 		   = "color".hashCode();
	protected static final int ELE_INDEX 		   = "index".hashCode();
	protected static final int ELE_NORMAL		   = "normal".hashCode();
	protected static final int ELE_VERTEX 		   = "vertex".hashCode();
	protected static final int ELE_MATRIX   	   = "matrix".hashCode();
	protected static final int ELE_CHILDREN 	   = "children".hashCode();
	protected static final int ELE_TEXTURE_COORDS  = "texture_coords".hashCode();
	protected static final int ELE_TEXTURE_INFO    = "texture_info".hashCode();
	
	protected static final int TYPE_FLOAT = 1;
	protected static final int TYPE_SHORT = 2;
	
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
		
		int capacity() {
			return mBuf.capacity();
		}
	};
	
	class ShortBuf {
		ByteBuffer mRoot;
		ShortBuffer mBuf;
		
		void set(ShortData data) {
			if (data == null) {
				mRoot = null;
				mBuf = null;
			} else {
    			mRoot = ByteBuffer.allocateDirect(data.size()*2);
    			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
    			mBuf = mRoot.asShortBuffer();
    			data.fill(mBuf);
			}
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
		
		int capacity() {
			return mBuf.capacity();
		}
	};
	
	protected FloatBuf mColorBuf = new FloatBuf();
	protected ShortBuf mIndexBuf = new ShortBuf();
    protected FloatBuf mNormalBuf = new FloatBuf();
    protected FloatBuf mVertexBuf = new FloatBuf();
    protected FloatBuf mTextureBuf = new FloatBuf();
	protected int mIndexMode = GL10.GL_TRIANGLES;
	protected Matrix4f mMatrix = null;
	protected ShapeData [] mChildren = null;
	protected TextureManager.Texture mTexture = null;
	
//	static final protected int FIXED_COLOR_ONE = 0x10000;
//	protected ShortData getColorFixed() { return null; }

	protected FloatData getColorData() { return null; }
	protected ShortData getIndexData() { return null; }
	protected FloatData getNormalData() { return null; }
	protected FloatData getVertexData() { return null; }
	protected FloatData getTextureData() { return null; }
	
	protected ShapeData [] _getChildren() { return null; }
	protected Matrix4f _getMatrix() { return null; }
	protected String _getTextureFilename() { return null; }
	
	// Returns the given object matrix if any.
	// Will return NULL if no matrix predefined transformation for the object
	// has been defined.
	protected Matrix4f getMatrix() {
		if (mMatrix == null) {
			mMatrix = _getMatrix();
		}
		return mMatrix;
	}
	
	protected ShapeData [] getChildren() {
		if (mChildren == null) {
			mChildren = _getChildren();
		}
		return mChildren;
	}
	
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
	
	public float getSizeXc() { return getMaxX()-getMinX(); }
	public float getSizeYc() { return getMaxY()-getMinY(); }
	public float getSizeZc() { return getMaxZ()-getMinZ(); }
	
	public FloatBuffer getColorBuf() { return mColorBuf.getBuf(); }
	public ShortBuffer getIndexBuf() { return mIndexBuf.getBuf(); }
	public FloatBuffer getVertexBuf() { return mVertexBuf.getBuf(); }
	public FloatBuffer getNormalBuf() { return mNormalBuf.getBuf(); }
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }
	
	public int getNumVertexes() { return mVertexBuf.capacity(); } 
	
	public interface FloatData {
		void fill(FloatBuffer buf);
		int size();
	}
	
	public interface MessageWriter {
		void msg(String tag, String msg);
	}
	
	public interface ShortData {
		void fill(ShortBuffer buf);
		int size();
	}
	
	public void compare(String tag, ShapeData other, MessageWriter msg) {
		FloatBuffer vertexBuf = getVertexBuf();
		FloatBuffer normalBuf = getNormalBuf();
		ShortBuffer indexBuf = getIndexBuf();
		FloatBuffer colorBuf = getColorBuf();
		FloatBuffer textureBuf = getTextureBuf();
		
		FloatBuffer vertexBufO = other.getVertexBuf();
		FloatBuffer normalBufO = other.getNormalBuf();
		ShortBuffer indexBufO = other.getIndexBuf();
		FloatBuffer colorBufO = other.getColorBuf();
		FloatBuffer textureBufO = other.getTextureBuf();
		
		if (ShapeUtils.compare(vertexBuf, vertexBufO, tag, "Vertex", msg) == 0) {
			msg.msg(tag, "vertex buffers identical");
		}
		if (ShapeUtils.compare(normalBuf, normalBufO, tag, "Normal", msg) == 0) {
			msg.msg(tag, "normal buffers identical");
		}
		if (ShapeUtils.compare(indexBuf, indexBufO, tag, "Index", msg) == 0) {
			msg.msg(tag, "index buffers identical");
		}
		if (ShapeUtils.compare(colorBuf, colorBufO, tag, "Color", msg) == 0) {
			msg.msg(tag, "color buffers identical");
		}
		if (ShapeUtils.compare(textureBuf, textureBufO, tag, "Texture", msg) == 0) {
			msg.msg(tag, "texture buffers identical");
		}
		ShapeUtils.compare(msg, tag, "MinX", getMinX(), other.getMinX());
		ShapeUtils.compare(msg, tag, "MinY", getMinY(), other.getMinY());
		ShapeUtils.compare(msg, tag, "MinZ", getMinZ(), other.getMinZ());
		ShapeUtils.compare(msg, tag, "MaxX", getMaxX(), other.getMaxX());
		ShapeUtils.compare(msg, tag, "MaxY", getMaxY(), other.getMaxY());
		ShapeUtils.compare(msg, tag, "MaxZ", getMaxZ(), other.getMaxZ());
		
		if (getMatrix() != null) {
			if (other.getMatrix() == null) {
				msg.msg(tag, "Second matrix was null, first wasn't");
			} else {
				for (int row = 0; row < 4; row++) {
					for (int col = 0; col < 4; col++) {
						StringBuffer sbuf = new StringBuffer();
						sbuf.append("M[");
						sbuf.append(row);
						sbuf.append("][");
						sbuf.append(col);
						sbuf.append("]");
						ShapeUtils.compare(msg, tag, sbuf.toString(), getMatrix().getValue(row, col), other.getMatrix().getValue(row, col));
					}
				}
			}
		} else if (other.getMatrix() != null) {
			msg.msg(tag, "First matrix was null, second matrix wasn't");
		}
		if (getChildren() != null) {
			if (other.getChildren() == null) {
				msg.msg(tag, "Second didn't have children");
			} else {
				if (getChildren().length != other.getChildren().length) {
					StringBuffer sbuf = new StringBuffer();
					sbuf.append("First had ");
					sbuf.append(getChildren().length);
					sbuf.append(" children, second had ");
					sbuf.append(other.getChildren().length);
					sbuf.append(" children.");
					msg.msg(tag, sbuf.toString());
				} else {
					for (int i = 0; i < getChildren().length; i++) {
						ShapeData child = getChildren()[i];
						ShapeData childO = other.getChildren()[i];
						StringBuffer sbuf = new StringBuffer();
						sbuf.append(tag);
						sbuf.append(".child");
						sbuf.append(i);
						child.compare(sbuf.toString(), childO, msg);
					}
				}
			}
		} else if (other.getChildren() != null) {
			msg.msg(tag, "First didn't have children");
		}
	}
	
	protected void allocBounds() {
		if (mBounds == null) {
    		mBounds = new float[6];
		}
	}
	
	public void computeBounds() {
		FloatBuffer buf = getVertexBuf();
		
		class ComputeBounds {
    		float minX = 0;
    		float minY = 0;
    		float minZ = 0;
    		float maxX = 0;
    		float maxY = 0;
    		float maxZ = 0;
    		boolean initialized = false;
    		
    		void apply(float x, float y, float z) {
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
		ComputeBounds computeBounds = new ComputeBounds();
		if (buf != null) {
    		while (buf.position() < buf.limit()) {
    			computeBounds.apply(buf.get(), buf.get(), buf.get());
    		}
		}
		if (getChildren() != null) {
			for (ShapeData child : getChildren()) {
				child.computeBounds();
				computeBounds.apply(child.getMinX(), child.getMinY(), child.getMinZ());
				computeBounds.apply(child.getMaxX(), child.getMaxY(), child.getMaxZ());
			}
		}
		allocBounds();
		setMinX(computeBounds.minX);
		setMinY(computeBounds.minY);
		setMinZ(computeBounds.minZ);
		setMaxX(computeBounds.maxX);
		setMaxY(computeBounds.maxY);
		setMaxZ(computeBounds.maxZ);
	}

	public void fill() {
		
		setVertexData(getVertexData());
		setNormalData(getNormalData());
		setIndexData(getIndexData());
		setColorData(getColorData());
		setTextureData(getTextureData());
		
		getMatrix();
		
		if (getChildren() != null) {
    		for (ShapeData child : getChildren()) {
    			child.fill();
    		}
		}
		
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
	
	public void onCreate(MatrixTrackingGL gl) {
		if (mTexture != null) {
			mTexture.use();
		}
		if (mChildren != null) {
			for (ShapeData shape : mChildren) {
				shape.onCreate(gl);
			}
		}
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		FloatBuffer fbuf;
		boolean didPush = false;
		
		Matrix4f matrix = getMatrix();
		if (matrix != null) {
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			didPush = true;
			
			Matrix4f curMatrix = gl.getMatrix();
			Matrix4f useMatrix = new Matrix4f(curMatrix).mult(matrix);
			gl.glLoadMatrix(useMatrix);
		}
		if ((fbuf = getVertexBuf()) != null) {
    		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fbuf);
		} else {
    		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
		if ((fbuf = getNormalBuf()) != null) {
    		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
    		gl.glNormalPointer(GL10.GL_FLOAT, 0, fbuf);
		} else {
    		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
		if ((fbuf = getColorBuf()) != null) {
    		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    		gl.glColorPointer(4, GL10.GL_FLOAT, 0, fbuf);
    		
    		// Not doing it this way anymore:
//    		gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
		} else {
    		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
		if (mTexture != null) {
			if ((fbuf = getTextureBuf()) != null) {
				mTexture.onDraw(gl);
				gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			}
		}
		ShortBuffer sbuf;
		if ((sbuf = getIndexBuf()) != null) {
			gl.glDrawElements(mIndexMode, sbuf.remaining(), GL10.GL_UNSIGNED_SHORT, sbuf);
		}
		if (mChildren != null) {
			for (ShapeData shape : mChildren) {
				shape.onDraw(gl);
			}
		}
		if (didPush) {
    		gl.glPopMatrix();
		}
	}
	
	public void onFinished(MatrixTrackingGL gl) {
		if (mTexture != null) {
			mTexture.done();
		}
		if (mChildren != null) {
			for (ShapeData shape : mChildren) {
				shape.onFinished(gl);
			}
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
	
	protected void readMatrix(DataInputStream dataStream) throws IOException {
		Matrix4f mat = new Matrix4f();
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				mat.setValue(row, col, dataStream.readFloat());
			}
		}
		mMatrix = mat;
	}
	
	protected void readTextureInfo(DataInputStream dataStream, TextureManager tm) throws IOException {
		mTexture = tm.getTexture(readString(dataStream));
	}
	
	protected void readChildren(DataInputStream dataStream, TextureManager tm) throws IOException, Exception {
		int numChildren = dataStream.readInt();
		mChildren = new ShapeData[numChildren];
		for (int i = 0; i < numChildren; i++) {
			mChildren[i] = new ShapeData();
			mChildren[i].readData(dataStream, tm);
		}
	}
	
	public void readData(InputStream inputStream, TextureManager tm) throws Exception, IOException {
		DataInputStream dataStream = new DataInputStream(inputStream);
		readData(dataStream, tm);
		dataStream.close();
	}
	
	protected void readData(DataInputStream dataStream, TextureManager tm) throws IOException, Exception {
		int eleType;
		int type;
		int size;
		ByteBuffer vbb;

		while (dataStream.available() > 0) {
			eleType = dataStream.readInt();
			
			if (eleType == ELE_FINISH) {
				return;
			}
			if (eleType == ELE_VERSION) {
				int version = dataStream.readInt();
				assert(version == FILE_VERSION);
			} else if (eleType == ELE_BOUNDS) {
				readBounds(dataStream);
			} else if (eleType == ELE_MATRIX) {
				readMatrix(dataStream);
			} else if (eleType == ELE_TEXTURE_INFO) {
				readTextureInfo(dataStream, tm);
			} else if (eleType == ELE_CHILDREN) {
				readChildren(dataStream, tm);
			} else if (eleType == ELE_VERTEX ||
					   eleType == ELE_NORMAL ||
					   eleType == ELE_INDEX ||
					   eleType == ELE_COLOR ||
					   eleType == ELE_TEXTURE_COORDS) {
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
				} else if (eleType == ELE_TEXTURE_COORDS) {
					if (type != TYPE_FLOAT) {
						throw new Exception("Expected float type for texture");
					}
					mTextureBuf.set(vbb);
				} 
			}
		}
	}
	
	public void readData(String filename, TextureManager tm) {
		File file = new File(filename);
		try {
			FileInputStream fileStream = new FileInputStream(file);
			readData(fileStream, tm);
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
	
	public void setTextureData(FloatData data) {
		if (data != null) {
			mTextureBuf = new FloatBuf();
    		mTextureBuf.set(data);
		}
	}
	
	public void setTexture(TextureManager.Texture texture) {
		mTexture = texture;
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
	
	protected void writeVersion(DataOutputStream dataStream) throws IOException {
		dataStream.writeInt(ELE_VERSION);
		dataStream.writeInt(FILE_VERSION);
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
	
	protected void writeMatrix(DataOutputStream dataStream) throws IOException {
		if (getMatrix() != null) {
    		dataStream.writeInt(ELE_MATRIX);
    		for (int row = 0; row < 4; row++) {
    			for (int col = 0; col < 4; col++) {
    				dataStream.writeFloat(mMatrix.getValue(row, col));
    			}
    		}
		}
	}
	
	protected void writeTextureInfo(DataOutputStream dataStream) throws IOException {
		if (_getTextureFilename() != null) {
    		dataStream.writeInt(ELE_TEXTURE_INFO);
    		writeString(dataStream, _getTextureFilename());
		}
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
        	
        	writeVersion(dataStream);
        	writeData(dataStream);
        	
        	dataStream.close();
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    		return false;
    	}
    	return true;
	}
	
	protected void writeData(DataOutputStream dataStream) throws IOException {
		writeBounds(dataStream);
		writeMatrix(dataStream);
		writeTextureInfo(dataStream);
		writeBuffer(dataStream, ELE_VERTEX, TYPE_FLOAT, mVertexBuf.getRootBuf());
		writeBuffer(dataStream, ELE_NORMAL, TYPE_FLOAT, mNormalBuf.getRootBuf());
		writeBuffer(dataStream, ELE_INDEX, TYPE_SHORT, mIndexBuf.getRootBuf());
		writeBuffer(dataStream, ELE_COLOR, TYPE_FLOAT, mColorBuf.getRootBuf());
		writeBuffer(dataStream, ELE_TEXTURE_COORDS, TYPE_FLOAT, mTextureBuf.getRootBuf());
		
    	if (getChildren() != null && mChildren.length > 0) {
    		dataStream.writeInt(ELE_CHILDREN);
    		dataStream.writeInt(mChildren.length);
    		for (int i = 0; i < mChildren.length; i++) {
    			mChildren[i].writeData(dataStream);
    		}
    	}
    	dataStream.writeInt(ELE_FINISH);
	}
	
	protected void writeString(DataOutputStream dataStream, String str) throws IOException {
		dataStream.writeShort(str.length());
		for (int i = 0; i < str.length(); i++) {
			dataStream.writeChar(str.charAt(i));
		}
	}
	
	protected String readString(DataInputStream dataStream) throws IOException {
		int len = dataStream.readShort();
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < len; i++) {
			sbuf.append(dataStream.readChar());
		}
		return sbuf.toString();
	}
}
