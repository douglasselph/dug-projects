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
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Vector3f;

// 
// Base class for defining shapes.
// Holds the core target variables that define shapes for use within OpenGL.
//
public class Shape {
	///////////////////////////////////////////////
	// CORE 
	///////////////////////////////////////////////
	protected static final int TYPE_FLOAT = 1;
	protected static final int TYPE_SHORT = 2;
	
	protected abstract class Buffer<BUFTYPE> {
		ByteBuffer mRoot;
		BUFTYPE mBuf;
		
		void set(dData<BUFTYPE> data) {
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
		
		abstract BUFTYPE asBuffer(ByteBuffer buf);
		abstract void rewind(BUFTYPE buf);
		abstract int capacity(BUFTYPE buf);
		abstract int getSize();
		abstract int getType();
		
		void set(ByteBuffer buf) {
			mRoot = buf;
			mBuf = asBuffer(buf);
		}
		
		BUFTYPE set(int size) {
			mRoot = ByteBuffer.allocateDirect(size*getSize());
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android platform
			mBuf = asBuffer(mRoot);
			return mBuf;
		}
		
		BUFTYPE getBuf() {
			if (mBuf != null) {
				rewind(mBuf);
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

	protected class FloatBuf extends Buffer<FloatBuffer> {
		@Override FloatBuffer asBuffer(ByteBuffer buf) { return buf.asFloatBuffer(); } @Override int capacity(FloatBuffer buf) { return buf.capacity(); }
		@Override void rewind(FloatBuffer buf) { buf.rewind(); }
		@Override int getSize() { return 4; }
		@Override int getType() { return TYPE_FLOAT; }
	};
	
	protected class ShortBuf extends Buffer<ShortBuffer> {
		@Override ShortBuffer asBuffer(ByteBuffer buf) { return buf.asShortBuffer(); }
		@Override int capacity(ShortBuffer buf) { return buf.capacity(); }
		@Override void rewind(ShortBuffer buf) { buf.rewind(); }
		@Override int getSize() { return 2; }
		@Override int getType() { return TYPE_SHORT; }
	};
	
	protected abstract class MyData<TARGET> {
		TARGET mTarget;
	};
	
	protected class Bone extends ShortBuf {
		String mName;
		int [] mJoints = null;
		int mJointParent = -1;
	};

	protected class Joint extends ShortBuf {
		int [] mBones = null;
	};
	
	public class Bounds {
		protected static final int MIN_X = 0;
		protected static final int MIN_Y = 1;
		protected static final int MIN_Z = 2;
		protected static final int MAX_X = 3;
		protected static final int MAX_Y = 4;
		protected static final int MAX_Z = 5;
		
		protected static final int SIZ = 6;

		float [] mBounds = new float[SIZ];
		
		protected float getMinX() { return mBounds[MIN_X]; }
		protected float getMinY() { return mBounds[MIN_Y]; }
		protected float getMinZ() { return mBounds[MIN_Z]; }
		protected float getMaxX() { return mBounds[MAX_X]; }
		protected float getMaxY() { return mBounds[MAX_Y]; }
		protected float getMaxZ() { return mBounds[MAX_Z]; }
		
		protected void setMaxX(float x) { mBounds[MAX_X] = x; }
		protected void setMaxY(float y) { mBounds[MAX_Y] = y; }
		protected void setMaxZ(float z) { mBounds[MAX_Z] = z; }
		protected void setMinX(float x) { mBounds[MIN_X] = x; }
		protected void setMinY(float y) { mBounds[MIN_Y] = y; }
		protected void setMinZ(float z) { mBounds[MIN_Z] = z; }
		
		public float getSizeXc() { return getMaxX()-getMinX(); }
		public float getSizeYc() { return getMaxY()-getMinY(); }
		public float getSizeZc() { return getMaxZ()-getMinZ(); }
		
		public float getMidX() { return (getMaxX()+getMinX())/2; }
		public float getMidY() { return (getMaxY()+getMinY())/2; }
		public float getMidZ() { return (getMaxZ()+getMinZ())/2; }
		
		public void compute() {
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
				for (Shape child : getChildren()) {
					child.getBounds().compute();
					computeBounds.apply(child.getBounds().getMinX(), child.getBounds().getMinY(), child.getBounds().getMinZ());
					computeBounds.apply(child.getBounds().getMaxX(), child.getBounds().getMaxY(), child.getBounds().getMaxZ());
				}
			}
			mBounds = new float[SIZ];
			
			setMinX(computeBounds.minX);
			setMinY(computeBounds.minY);
			setMinZ(computeBounds.minZ);
			setMaxX(computeBounds.maxX);
			setMaxY(computeBounds.maxY);
			setMaxZ(computeBounds.maxZ);
		}
		
	};
	
	protected FloatBuf mColorBuf = new FloatBuf();
	protected ShortBuf mIndexBuf = new ShortBuf();
    protected FloatBuf mNormalBuf = new FloatBuf();
    protected FloatBuf mVertexBuf = new FloatBuf();
    protected FloatBuf mTextureBuf = new FloatBuf();
    protected Bone [] mBones = null;
    protected Joint [] mJoints = null;
	protected Bounds mBounds = new Bounds();
    
	protected int mIndexMode = GL10.GL_TRIANGLES;
	protected Matrix4f mMatrix = null;
	protected Shape [] mChildren = null;
	protected TextureManager.Texture mTexture = null;
	
	public Shape [] getChildren() { return mChildren; }
	protected Matrix4f getMatrix() { return mMatrix; }
	
	protected int getFrontFace() { return GL10.GL_CCW; }
	protected int getCullFace() { return GL10.GL_BACK; }
	
	public Bounds getBounds() { return mBounds; }
	
	public FloatBuffer getColorBuf() { return mColorBuf.getBuf(); }
	public ShortBuffer getIndexBuf() { return mIndexBuf.getBuf(); }
	public FloatBuffer getVertexBuf() { return mVertexBuf.getBuf(); }
	public FloatBuffer getNormalBuf() { return mNormalBuf.getBuf(); }
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }
	
	public int getNumVertexes() { return mVertexBuf.capacity(); } 
	
//	static final protected int FIXED_COLOR_ONE = 0x10000;
//	protected ShortData getColorFixed() { return null; }
	

	// Other helper functions:
	public interface MessageWriter {
		void msg(String tag, String msg);
	}
	
	public void compare(String tag, Shape other, MessageWriter msg) {
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
		if (mBones != null) {
			if (other.mBones != null) {
				if (mBones.length != other.mBones.length) {
					StringBuffer sbuf = new StringBuffer();
					sbuf.append("Different number of bones ");
					sbuf.append(mBones.length);
					sbuf.append(" != ");
					sbuf.append(other.mBones.length);
					msg.msg(tag, sbuf.toString());
				} else {
					for (int i = 0; i < mBones.length; i++) {
						Bone bone = mBones[i];
						Bone boneO = other.mBones[i];
						
						if (!bone.mName.equals(boneO.mName)) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(" name ");
							sbuf.append(bone.mName);
							sbuf.append(" != ");
							sbuf.append(boneO.mName);
							msg.msg(tag, sbuf.toString());
						}
						if (bone.mJoints != null && other.mJoints != null) {
							if (bone.mJoints.length != boneO.mJoints.length) {
								StringBuffer sbuf = new StringBuffer();
								sbuf.append("Bone ");
								sbuf.append(i);
								sbuf.append(" #joints ");
								sbuf.append(bone.mJoints.length);
								sbuf.append(" != ");
								sbuf.append(boneO.mJoints.length);
								msg.msg(tag, sbuf.toString());
							} else {
								for (int j = 0; j < bone.mJoints.length; j++) {
									if (bone.mJoints[j] != boneO.mJoints[j]) {
										StringBuffer sbuf = new StringBuffer();
										sbuf.append("Bone ");
										sbuf.append(i);
										sbuf.append(" Joint ");
										sbuf.append(j);
										sbuf.append(" ");
										sbuf.append(bone.mJoints[j]);
										sbuf.append(" != ");
										sbuf.append(boneO.mJoints[j]);
										msg.msg(tag, sbuf.toString());
									}
								}
							}
						} else if (bone.mJoints != null && other.mJoints == null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(": first has joints, second didn't");
							msg.msg(tag, sbuf.toString());
						} else if (bone.mJoints == null && other.mJoints != null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(": second has joints, first didn't");
							msg.msg(tag, sbuf.toString());
						}
						if (bone.mJointParent != boneO.mJointParent) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(" JointParent ");
							sbuf.append(bone.mJointParent);
							sbuf.append(" != ");
							sbuf.append(boneO.mJointParent);
							msg.msg(tag, sbuf.toString());
						}
						{
    						StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							
    						if (ShapeUtils.compare(bone.getBuf(), boneO.getBuf(), tag, sbuf.toString(), msg) == 0) {
    							StringBuffer sbuf2 = new StringBuffer();
    							sbuf2.append(sbuf.toString());
    							sbuf2.append(" buffer identical");
    							msg.msg(tag, sbuf2.toString());
    						}
						}
					}
				}
			} else {
				msg.msg(tag, "Second bones was null, first wasn't");
			}
		} else if (other.mBones != null) {
			msg.msg(tag, "First bones was null, second wasn't");
		}
		if (mJoints != null) {
			if (other.mJoints != null) {
				if (mJoints.length != other.mJoints.length) {
					StringBuffer sbuf = new StringBuffer();
					sbuf.append("Different number of joints ");
					sbuf.append(mJoints.length);
					sbuf.append(" != ");
					sbuf.append(other.mJoints.length);
					msg.msg(tag, sbuf.toString());
				} else {
					for (int i = 0; i < mJoints.length; i++) {
						Joint joint = mJoints[i];
						Joint jointO = other.mJoints[i];
						
						if (joint.mBones != null && jointO.mBones != null) {
							if (joint.mBones.length != jointO.mBones.length) {
								StringBuffer sbuf = new StringBuffer();
								sbuf.append("Joint ");
								sbuf.append(i);
								sbuf.append(" #bones ");
								sbuf.append(joint.mBones.length);
								sbuf.append(" != ");
								sbuf.append(jointO.mBones.length);
								msg.msg(tag, sbuf.toString());
							} else {
								for (int j = 0; j < joint.mBones.length; j++) {
									if (joint.mBones[j] != jointO.mBones[j]) {
										StringBuffer sbuf = new StringBuffer();
										sbuf.append("Joint ");
										sbuf.append(i);
										sbuf.append(" Bone ");
										sbuf.append(j);
										sbuf.append(" ");
										sbuf.append(joint.mBones[j]);
										sbuf.append(" != ");
										sbuf.append(jointO.mBones[j]);
										msg.msg(tag, sbuf.toString());
									}
								}
							}
						} else if (joint.mBones != null && jointO.mBones == null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Joint ");
							sbuf.append(i);
							sbuf.append(": first has bones, second didn't");
							msg.msg(tag, sbuf.toString());
						} else if (joint.mBones == null && jointO.mBones != null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Joint ");
							sbuf.append(i);
							sbuf.append(": second has bones, first didn't");
							msg.msg(tag, sbuf.toString());
						}
						{
    						StringBuffer sbuf = new StringBuffer();
							sbuf.append("Joint ");
							sbuf.append(i);
							
    						if (ShapeUtils.compare(joint.getBuf(), jointO.getBuf(), tag, sbuf.toString(), msg) == 0) {
    							StringBuffer sbuf2 = new StringBuffer();
    							sbuf2.append(sbuf.toString());
    							sbuf2.append(" buffer identical");
    							msg.msg(tag, sbuf2.toString());
    						}
						}
					}
				}
			} else {
				msg.msg(tag, "Second joints was null, first wasn't");
			}
		} else if (other.mJoints != null) {
			msg.msg(tag, "First joints was null, second wasn't");
		}
		ShapeUtils.compare(msg, tag, "MinX", getBounds().getMinX(), other.getBounds().getMinX());
		ShapeUtils.compare(msg, tag, "MinY", getBounds().getMinY(), other.getBounds().getMinY());
		ShapeUtils.compare(msg, tag, "MinZ", getBounds().getMinZ(), other.getBounds().getMinZ());
		ShapeUtils.compare(msg, tag, "MaxX", getBounds().getMaxX(), other.getBounds().getMaxX());
		ShapeUtils.compare(msg, tag, "MaxY", getBounds().getMaxY(), other.getBounds().getMaxY());
		ShapeUtils.compare(msg, tag, "MaxZ", getBounds().getMaxZ(), other.getBounds().getMaxZ());
		
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
						Shape child = getChildren()[i];
						Shape childO = other.getChildren()[i];
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
	
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		if (mVertexBuf != null) {
			sbuf.append(ShapeUtils.toString("vertexbuf", mVertexBuf.getBuf()));
		}
		if (mIndexBuf != null) {
			sbuf.append(ShapeUtils.toString("indexbuf", mIndexBuf.getBuf()));
		}
		return sbuf.toString();
	}
	
	public Vector3f getMidPoint() {
		Bounds bounds = getBounds();
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(), bounds.getMidZ());
		Matrix4f matrix = getMatrix();
		matrix.applyPost(midPoint);
		return midPoint;
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
	
	///////////////////////////////////////
	// DEFINE
	///////////////////////////////////////
	
	public interface dData<BUFTYPE> {
		void fill(BUFTYPE buf);
		int size();
	};
	
	public interface dFloatBuf extends dData<FloatBuffer> {
	};
	
	public interface dShortBuf extends dData<ShortBuffer> {
	};
	
	public class dBone implements dShortBuf {
		public String getName() { return null; }
		public void fill(ShortBuffer buf) {}
		public int size() { return 0; }
		public int [] getJoints() { return null; }
		public int getJointParent() { return -1; }
	};

	public class dJoint implements dShortBuf {
		public void fill(ShortBuffer buf) {}
		public int size() { return 0; }
		public int [] getBones() { return null; }
	};

	protected dFloatBuf dGetColorDef() { return null; }
	protected dShortBuf dGetIndexDef() { return null; }
	protected dFloatBuf dGetNormalDef() { return null; }
	protected dFloatBuf dGetVertexDef() { return null; }
	protected dFloatBuf dGetTextureDef() { return null; }
	protected dBone [] dGetBonesDef() { return null; }
	protected dJoint [] dGetJointsDef() { return null; }
	
	protected float dGetMinX() { return mBounds.getMinX(); }
	protected float dGetMinY() { return mBounds.getMinY(); }
	protected float dGetMinZ() { return mBounds.getMinZ(); }
	protected float dGetMaxX() { return mBounds.getMaxX(); }
	protected float dGetMaxY() { return mBounds.getMaxY(); }
	protected float dGetMaxZ() { return mBounds.getMaxZ(); }
	
	protected String dGetTextureFilename() { return null; }
	protected Color4f dGetColor() { return null; }
	protected Matrix4f dGetMatrix() { return null; }
	protected Shape [] dGetChildren() { return null; }
	
	public void fill() {

		setVertexData(dGetVertexDef());
		setNormalData(dGetNormalDef());
		setIndexData(dGetIndexDef());
		setColorData(dGetColorDef());
		setTextureData(dGetTextureDef());

		setBonesData(dGetBonesDef());
		setJointsData(dGetJointsDef());
		
		getBounds().setMinX(dGetMinX());
		getBounds().setMinY(dGetMinY());
		getBounds().setMinZ(dGetMinZ());
		getBounds().setMaxX(dGetMaxX());
		getBounds().setMaxY(dGetMaxY());
		getBounds().setMaxZ(dGetMaxZ());
		
		mMatrix = dGetMatrix();
		if (mMatrix == null) {
			mMatrix = new Matrix4f();
		}
		Shape [] children = dGetChildren();
		if (children != null) {
			mChildren = children;
			for (Shape child : mChildren) {
				child.fill();
			}
		}

		// It is arguably faster and easier to use floats
		// because modern hardware supports colors floats.
		// If not, then it is faster to use GL_FIXED.
		// Right now, this code is optimized for modern hardware.

		//			setColorData(getColorFixed()); // Note: uses FIXED, which means one is 0x10000.
	}

	//		public void setColorData(ShortData data) {
	//			if (data != null) {
	//	    		mColorBuf = fill(data);
	//			}
	//		}

	public void setColorData(dFloatBuf data) {
		if (data != null) {
			mColorBuf = new FloatBuf();
			mColorBuf.set(data);
		}
	}

	public void setTextureData(dFloatBuf data) {
		if (data != null) {
			mTextureBuf = new FloatBuf();
			mTextureBuf.set(data);
		}
	}

	public void setTexture(TextureManager.Texture texture) {
		mTexture = texture;
	}

	public void setIndexData(dShortBuf data) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
	}

	public void setIndexData(dShortBuf data, int mode) {
		mIndexBuf = new ShortBuf();
		mIndexBuf.set(data);
		mIndexMode = mode;
	}

	public void setNormalData(dFloatBuf data) {
		mNormalBuf = new FloatBuf();
		mNormalBuf.set(data);
	}

	public void setVertexData(dFloatBuf data) {
		mVertexBuf = new FloatBuf();
		mVertexBuf.set(data);
	}

	public void setBonesData(dBone [] bones) {
		if (bones == null) {
			mBones = null;
		} else {
			mBones = new Bone[bones.length];
			for (int i = 0; i < bones.length; i++) {
				Bone tgt = new Bone();
				dBone src = bones[i];
				tgt.mName = src.getName();
				int [] joints = src.getJoints();
				if (joints != null) {
					tgt.mJoints = new int[joints.length];
					for (int j = 0; j < joints.length; j++) {
						tgt.mJoints[j] = joints[j];
					}
				}
				tgt.mJointParent = src.getJointParent();
				tgt.set(src);
				mBones[i] = tgt;
			}
		}
	}

	public void setJointsData(dJoint [] joints) {
		if (joints == null) {
			mJoints = null;
		} else {
			mJoints = new Joint[joints.length];
			for (int i = 0; i < joints.length; i++) {
				Joint tgt = new Joint();
				dJoint src = joints[i];
				int [] bones = src.getBones();
				if (bones != null) {
					tgt.mBones = new int[bones.length];
					for (int j = 0; j < bones.length; j++) {
						tgt.mBones[j] = bones[j];
					}
				}
				tgt.set(src);
				mJoints[i] = tgt;
			}
		}
	}

	///////////////////////////////////////
	// FILE
	///////////////////////////////////////
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
	protected static final int ELE_BONES    	   = "bones".hashCode();
	protected static final int ELE_JOINTS    	   = "joints".hashCode();
	protected static final int ELE_BONE    	   	   = "bone".hashCode();
	protected static final int ELE_JOINT    	   = "joint".hashCode();

	//
	// TOP LEVEL
	//
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

	protected void writeData(DataOutputStream dataStream) throws IOException, Exception {
		writeBounds(dataStream);
		writeMatrix(dataStream);
		writeTextureInfo(dataStream);
		writeData(dataStream, ELE_VERTEX, mVertexBuf);
		writeData(dataStream, ELE_NORMAL, mNormalBuf);
		writeData(dataStream, ELE_INDEX, mIndexBuf);
		writeData(dataStream, ELE_COLOR, mColorBuf);
		writeData(dataStream, ELE_TEXTURE_COORDS, mTextureBuf);
		writeBones(dataStream);
		writeJoints(dataStream);

		if (getChildren() != null && mChildren.length > 0) {
			dataStream.writeInt(ELE_CHILDREN);
			dataStream.writeInt(mChildren.length);
			for (int i = 0; i < mChildren.length; i++) {
				mChildren[i].writeData(dataStream);
			}
		}
		dataStream.writeInt(ELE_FINISH);
	}

	protected void writeData(DataOutputStream dataStream, int eleType, FloatBuf buf) throws IOException, Exception {
		if (buf.getRootBuf() != null) {
    		dataStream.writeInt(eleType);
    		buf.writeBuffer(dataStream);
		}
	}

	protected void writeData(DataOutputStream dataStream, int eleType, ShortBuf buf) throws IOException, Exception {
		if (buf.getRootBuf() != null) {
			dataStream.writeInt(eleType);
			buf.writeBuffer(dataStream);
		}
	}

	protected void writeVersion(DataOutputStream dataStream) throws IOException {
		dataStream.writeInt(ELE_VERSION);
		dataStream.writeInt(FILE_VERSION);
	}

	public void readData(InputStream inputStream, TextureManager tm) throws Exception, IOException {
		DataInputStream dataStream = new DataInputStream(inputStream);
		readData(dataStream, tm);
		dataStream.close();
	}

	protected void readData(DataInputStream dataStream, TextureManager tm) throws IOException, Exception {
		int eleType;

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
			} else if (eleType == ELE_VERTEX) {
				mVertexBuf.readBuffer(dataStream);
			} else if (eleType == ELE_NORMAL) {
				mNormalBuf.readBuffer(dataStream);
			} else if (eleType == ELE_INDEX) {
				mIndexBuf.readBuffer(dataStream);
			} else if (eleType == ELE_COLOR) {
				mColorBuf.readBuffer(dataStream);
			} else if (eleType == ELE_TEXTURE_COORDS) {
				mTextureBuf.readBuffer(dataStream);
			} else if (eleType == ELE_BONES) {
				readBones(dataStream);
			} else if (eleType == ELE_JOINTS) {
				readJoints(dataStream);
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

	protected void readChildren(DataInputStream dataStream, TextureManager tm) throws IOException, Exception {
		int numChildren = dataStream.readInt();
		mChildren = new Shape[numChildren];
		for (int i = 0; i < numChildren; i++) {
			Shape child = new Shape();
			child.readData(dataStream, tm);
			mChildren[i] = child;
		}
	}

	// 
	// BOUNDS
	// 
	protected void writeBounds(DataOutputStream dataStream) throws IOException {
		dataStream.writeInt(ELE_BOUNDS);
		dataStream.writeFloat(getBounds().getMinX());
		dataStream.writeFloat(getBounds().getMinY());
		dataStream.writeFloat(getBounds().getMinZ());
		dataStream.writeFloat(getBounds().getMaxX());
		dataStream.writeFloat(getBounds().getMaxY());
		dataStream.writeFloat(getBounds().getMaxZ());
	}

	protected void readBounds(DataInputStream dataStream) throws IOException {
		mBounds.setMinX(dataStream.readFloat());
		mBounds.setMinY(dataStream.readFloat());
		mBounds.setMinZ(dataStream.readFloat());
		mBounds.setMaxX(dataStream.readFloat());
		mBounds.setMaxY(dataStream.readFloat());
		mBounds.setMaxZ(dataStream.readFloat());
	}

	// 
	// MATRIX
	// 
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

	protected void readMatrix(DataInputStream dataStream) throws IOException {
		Matrix4f mat = new Matrix4f();
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				mat.setValue(row, col, dataStream.readFloat());
			}
		}
		mMatrix = mat;
	}

	//
	// TEXTURE
	//
	protected void writeTextureInfo(DataOutputStream dataStream) throws IOException {
		if (dGetTextureFilename() != null) {
			dataStream.writeInt(ELE_TEXTURE_INFO);
			ShapeUtils.writeString(dataStream, dGetTextureFilename());
		}
	}

	protected void readTextureInfo(DataInputStream dataStream, TextureManager tm) throws IOException {
		mTexture = tm.getTexture(ShapeUtils.readString(dataStream));
	}

	//
	// BONES
	//
	protected void writeBones(DataOutputStream dataStream) throws IOException, Exception {
		if (mBones != null) {
			dataStream.writeInt(ELE_BONES);
			dataStream.writeInt(mBones.length);
			for (Bone bone : mBones) {
				writeBone(dataStream, bone);
			}
		}
	}

	protected void readBones(DataInputStream dataStream) throws IOException, Exception {
		int num = dataStream.readInt();
		mBones = new Bone[num];
		for (int i = 0; i < num; i++) {
			mBones[i] = readBone(dataStream);
		}
	}

	//
	// BONE
	//
	protected void writeBone(DataOutputStream dataStream, Bone bone) throws IOException, Exception {
		dataStream.writeInt(ELE_BONE);
		ShapeUtils.writeString(dataStream, bone.mName);
		if (bone.mJoints != null) {
			dataStream.writeInt(bone.mJoints.length);
			for (int joint : bone.mJoints) {
				dataStream.writeInt(joint);
			}
		} else {
			dataStream.writeInt(0);
		}
		dataStream.writeInt(bone.mJointParent);
		bone.writeBuffer(dataStream);
	}

	protected Bone readBone(DataInputStream dataStream) throws IOException, Exception {
		int type = dataStream.readInt(); // ELE_BONE
		if (type != ELE_BONE) {
			throw new Exception("Exception ELE_BONE");
		}
		Bone bone = new Bone();
		bone.mName = ShapeUtils.readString(dataStream);
		int numJoints = dataStream.readInt();
		if (numJoints > 0) {
			bone.mJoints = new int[numJoints];
			for (int i = 0; i < numJoints; i++) {
				bone.mJoints[i] = dataStream.readInt();
			}
		}
		bone.mJointParent = dataStream.readInt();
		bone.readBuffer(dataStream);
		return bone;
	}

	//
	// JOINTS
	//
	protected void writeJoints(DataOutputStream dataStream) throws IOException, Exception {
		if (mJoints != null) {
			dataStream.writeInt(ELE_JOINTS);
			dataStream.writeInt(mJoints.length);

			for (Joint joint : mJoints) {
				writeJoint(dataStream, joint);
			}
		}
	}

	protected void readJoints(DataInputStream dataStream) throws IOException, Exception {
		int num = dataStream.readInt();
		mJoints = new Joint[num];
		for (int i = 0; i < num; i++) {
			mJoints[i] = readJoint(dataStream);
		}
	}

	//
	// JOINT 
	//
	protected void writeJoint(DataOutputStream dataStream, Joint joint) throws IOException, Exception {
		dataStream.writeInt(ELE_JOINT);
		if (joint.mBones != null) {
			dataStream.writeInt(joint.mBones.length);
			for (int bone : joint.mBones) {
				dataStream.writeInt(bone);
			}
		} else {
			dataStream.writeInt(0);
		}
		joint.writeBuffer(dataStream);
	}

	protected Joint readJoint(DataInputStream dataStream) throws IOException, Exception {
		int type = dataStream.readInt(); // ELE_JOINT
		if (type != ELE_JOINT) {
			throw new Exception("Exception ELE_JOINT");
		}
		Joint joint = new Joint();
		int numBones = dataStream.readInt();
		if (numBones > 0) {
			joint.mBones = new int[numBones];
			for (int i = 0; i < numBones; i++) {
				joint.mBones[i] = dataStream.readInt();
			}
		}
		joint.readBuffer(dataStream);
		return joint;
	}

	///////////////////////////////////////
	// OpenGL
	///////////////////////////////////////

	public void onCreate(MatrixTrackingGL gl) {
		if (mTexture != null) {
			mTexture.use();
		}
		if (mChildren != null) {
			for (Shape shape : mChildren) {
				shape.onCreate(gl);
			}
		}
	}

	public void onDraw(MatrixTrackingGL gl) {
		FloatBuffer fbuf;
		boolean didPush = false;

		if (!hasColorArray()) {
			Color4f color = dGetColor();
			if (color != null) {
				gl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			}
		}

		if (getFrontFace() != gl.getFrontFace()) {
			gl.glFrontFace(getFrontFace());
		}
		if (hasTextureArray()) {
			gl.setCullFace(0);
		} else {
			gl.setCullFace(getCullFace());
		}
		gl.glDisable(GL10.GL_BLEND);

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
				//	    		gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
		} else {
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
		if (mTexture != null) {
			if ((fbuf = getTextureBuf()) != null) {
				mTexture.onDraw(gl, fbuf);
			} else {
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			}
		}
		ShortBuffer sbuf;
		if ((sbuf = getIndexBuf()) != null) {
			gl.glDrawElements(mIndexMode, sbuf.remaining(), GL10.GL_UNSIGNED_SHORT, sbuf);
		}
		if (mChildren != null) {
			for (Shape shape : mChildren) {
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
			for (Shape shape : mChildren) {
				shape.onFinished(gl);
			}
		}
	}
}
