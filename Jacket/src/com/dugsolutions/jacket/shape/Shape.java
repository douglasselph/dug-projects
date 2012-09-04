package com.dugsolutions.jacket.shape;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.image.TextureManager;
import com.dugsolutions.jacket.math.Bounds3D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.ComputeBounds;
import com.dugsolutions.jacket.math.Matrix4f;
import com.dugsolutions.jacket.math.MatrixTrackingGL;
import com.dugsolutions.jacket.math.Point;
import com.dugsolutions.jacket.math.Quaternion;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.shape.BufferUtils.FloatBuf;
import com.dugsolutions.jacket.shape.BufferUtils.ShortBuf;
import com.dugsolutions.jacket.shape.BufferUtils.ShortBufSortedRange;
import com.dugsolutions.jacket.shape.BufferUtils.dFloatBuf;
import com.dugsolutions.jacket.shape.BufferUtils.dShortBuf;
import com.dugsolutions.jacket.shape.Shape.AnimSet.AnimControl;

// 
// Base class for defining shapes.
// Holds the core target variables that define shapes for use within OpenGL.
//
public class Shape {
	///////////////////////////////////////////////
	// CORE 
	///////////////////////////////////////////////
	
	public class ShapeBounds extends Bounds3D {
		
		public void compute() {
			ComputeBounds computeBounds = new ComputeBounds();
			FloatBuffer buf = getVertexBuf();
			
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
			set(computeBounds);
		}		
	};

//	protected abstract class MyData<TARGET> {
//		TARGET mTarget;
//	};
	
	public class AnimSet {
		public class AnimControl {
			float [] mPairs;
			
			public AnimControl(float [] pairs) {
				mPairs = pairs;
			}
			
			public float get(int i) {
				return mPairs[i];
			}
			
			public int num() {
				return mPairs.length;
			}
			
			// Return time/value pairs before and after indicated time.
			// If time is on a time/value pair then only return matching
			// time/value pair.
			//
			// If before the first time, returns first time/value pair.
			// If after last time, return last time/value pair.
			//
			// If no time/value pairs, returns null.
			public Point [] getPairs(float time) {
				if (mPairs.length < 2) {
					return null;
				}
				int before_index = 0;
				
				for (int i = 2; i < mPairs.length-1; i += 2) {
					if (mPairs[i] > time) {
						break;
					}
					before_index = i;
				}
				if ((mPairs[before_index] >= time) || (before_index+3 >= mPairs.length)) {
					Point [] pairs = new Point[1];
					pairs[0] = new Point(mPairs[before_index], mPairs[before_index+1]);
					return pairs;
				} else if (mPairs[before_index+2] == time) {
					Point [] pairs = new Point[1];
					pairs[0] = new Point(mPairs[before_index+2], mPairs[before_index+3]);
					return pairs;
				}
				Point [] pairs = new Point[2];
				pairs[0] = new Point(mPairs[before_index], mPairs[before_index+1]);
				pairs[1] = new Point(mPairs[before_index+2], mPairs[before_index+3]);
				return pairs;
			}
			
			float getFirstTime() {
				return mPairs[0];
			}
			
			float getLastTime() {
				return mPairs[mPairs.length-2];
			}
			
			// Remove all entries before the given startTime and
			// after the given endTime.
			void cullToTime(float startTime, float endTime) {
				int start_index = -1;
				int end_index = -1;
				
				for (int i = 0; i < mPairs.length; i += 2) {
					if (start_index == -1) {
						if (mPairs[i] >= startTime) {
							start_index = i;
						} else {
							continue;
						}
					}
					if (mPairs[i] <= endTime) {
						end_index = i;
					} else {
						break;
					}
				}
				int num = end_index - start_index + 1;
				if (num <= 0) {
					mPairs = new float[0];
				} else {
    				float [] values = new float[num+1];
    				for (int i = start_index; i <= end_index+1; i++) {
    					values[i] = mPairs[i];
    				}
    				mPairs = values;
				}
			}
		
			///////////////////////////////////////
			// Alternative to limit(), hasRemaining(), and get() functions
			// which includes  connected bones as well.
			//////////////////////////////////////
			
//			int limit_incChildBones() {
//			}
//			
//			boolean hasRemaining_incChildBones() {
//			}
//			
//			short get_incChildBones() {
//			}
		};
		
		protected String mName;
		protected HashMap<AnimType,AnimControl> mControls = new HashMap<AnimType,AnimControl>();
		protected float mAnimStartTime;
		protected float mAnimEndTime;
		protected AnimControlOp mControlOp  = AnimControlOp.ControlBoneOnly;
		
		public AnimSet(String name) {
			mName = name;
		}
		
		public int getNumControls() {
			if (mControls == null) {
				return 0;
			}
			return mControls.keySet().size();
		}
		
		public Set<AnimType> getControlKeys() {
			return mControls.keySet();
		}
		
		public AnimControl getAnim(AnimType key) {
			return mControls.get(key);
		}

		public void computeBounds() {
			boolean first = true;
			for (AnimType type : mControls.keySet()) {
				AnimControl anim = mControls.get(type);
				if (first) {
					mAnimStartTime = anim.getFirstTime();
					mAnimEndTime = anim.getLastTime();
					first = false;
				} else {
					if (anim.getFirstTime() < mAnimStartTime) {
						mAnimStartTime = anim.getFirstTime();
					} else if (anim.getLastTime() > mAnimEndTime) {
						mAnimEndTime = anim.getLastTime();
					}
				}
			}
		}
		
		public void setAnimKnotPts(AnimType type, float [] pts) {
			mControls.put(type,new AnimControl(pts));
		}
		
		public float getStartTime() {
			return mAnimStartTime;
		}
		
		public float getEndTime() {
			return mAnimEndTime;
		}
		
		public void setControlOp(AnimControlOp op) {
			mControlOp = op;
		}
		
		public AnimControlOp getControlOp() {
			return mControlOp;
		}
		
		public void cullToTime(float startTime, float endTime) {
			for (AnimType type : mControls.keySet()) {
				mControls.get(type).cullToTime(startTime, endTime);
			}
			computeBounds();
		}
	};

	public class Bone extends ShortBufSortedRange {
		protected String mName;
		protected int [] mJoints = null;
		protected int mJointParent = -1;
		protected Bounds3D mBounds = null;
		protected ArrayList<AnimSet> mAnim = null;

		public String getName() {
			return mName;
		}
		
		public int getNumJoints() {
			return mJoints.length;
		}
	
		public Joint getJoint(int index) {
			if (index >= 0 && index < mJoints.length) {
				return Shape.this.getJoint(mJoints[index]);
			}
			return null;
		}
		
		public Joint getJointParent() {
			return Shape.this.getJoint(mJointParent);
		}
		
		public int getNumAnim() {
			if (mAnim == null) {
				return 0;
			}
			return mAnim.size();
		}
		
		public ArrayList<AnimSet> getAnimSet() {
			return mAnim;
		}
		
		public Bounds3D getBounds() { return mBounds; }
		
		public void computeBounds() {
			FloatBuffer fbuf = getVertexBuf();
			int index;
			float x, y, z;
			
			ComputeBounds computeBounds = new ComputeBounds();
			
			rewind();
			
			fbuf.rewind();
			while (hasRemaining()) {
				index = get();
				fbuf.position(index*3);
				x = fbuf.get();
				y = fbuf.get();
				z = fbuf.get();
				computeBounds.apply(x, y, z);
			}
			mBounds = computeBounds.getBounds();
		}
		
		public AnimSet allocAnimSet(String name) {
			AnimSet set = new AnimSet(name);
			if (mAnim == null) {
				mAnim = new ArrayList<AnimSet>();
			}
			mAnim.add(set);
			return set;
		}
		
		public ArrayList<AnimSet> getAnimSets() {
			return mAnim;
		}
		
		public AnimSet getAnimSet(String name) {
			if (mAnim == null) {
				return null;
			}
			for (AnimSet set : mAnim) {
				if (set.mName.equals(name)) {
					return set;
				}
			}
			return null;
		}
		
		public AnimSet getAnimSet(int index) {
			if ((mAnim == null) || (index < 0) || (index >= mAnim.size())) {
				return null;
			}
			return mAnim.get(index);
		}

	};

	public class Joint extends ShortBuf {
		protected int [] mBones = null;
		
		public int getNumBones() {
			return mBones.length;
		}
		
		public Bone getBone(int index) {
			if (index >=0 && index < mBones.length) {
				return Shape.this.getBone(mBones[index]);
			}
			return null;
		}
	};
	
	protected FloatBuf mColorBuf = new FloatBuf();
	protected ShortBuf mIndexBuf = new ShortBuf();
    protected FloatBuf mNormalBuf = new FloatBuf();
    protected FloatBuf mVertexBuf = new FloatBuf();
    protected FloatBuf mTextureBuf = new FloatBuf();
    protected Bone [] mBones = null;
    protected Joint [] mJoints = null;
	protected ShapeBounds mBounds = new ShapeBounds();
    
	protected int mIndexMode = GL10.GL_TRIANGLES;
	protected Matrix4f mMatrix = null;
	protected Matrix4f mMatrixMod = null;
	protected ArrayList<Shape> mChildren = null;
	protected TextureManager.Texture mTexture = null;
	protected int mCullFace = GL10.GL_BACK;
	protected Color4f mColor = null;
	protected Color4f mColorOutline = null;
	
	public Shape getChild(int i) { return mChildren.get(i); }
	public ArrayList<Shape> getChildren() { return mChildren; }
	
	protected int getFrontFace() { return GL10.GL_CCW; }
	public int getCullFace() { return mCullFace; }
	
	public Bone [] getBones() { return mBones; }
	public Joint [] getJoints() { return mJoints; }
	
	public ShapeBounds getBounds() { return mBounds; }
	
	public Bone getAnimBone() {
		if (getBones() != null) {
			for (Bone bone : getBones()) {
				if (bone.getNumAnim() > 0) {
					return bone;
				}
			}
		}
		return null;
	}
	
	public int getNumBones() {
		if (mBones == null) {
			return 0;
		} else {
			return mBones.length;
		}
	}
	
	public int getNumJoints() {
		if (mJoints == null) {
			return 0;
		} else {
			return mJoints.length;
		}
	}
	
	public Joint getJoint(int index) {
		if (index >= 0 && index < mJoints.length) {
			return mJoints[index];
		}
		return null;
	}
	
	public Bone getBone(int index) {
		if (index >= 0 && index < mBones.length) {
			return mBones[index];
		}
		return null;
	}
	
	public ArrayList<Bone> getBones(float x, float y, float z) {
		if (mBones == null) {
			return null;
		}
		ArrayList<Bone> list = new ArrayList<Bone>();
		for (Bone bone : mBones) {
			if (bone.getBounds().within(x, y, z)) {
				list.add(bone);
			}
		}
		return list;
	}
	
	public ArrayList<Bone> getBones(float x, float y) {
		if (mBones == null) {
			return null;
		}
		ArrayList<Bone> list = new ArrayList<Bone>();
		for (Bone bone : mBones) {
			if (bone.getBounds().within(x, y)) {
				list.add(bone);
			}
		}
		return list;
	}
	
	public FloatBuffer getColorBuf() { return mColorBuf.getBuf(); }
	public ShortBuffer getIndexBuf() { return mIndexBuf.getBuf(); }
	public FloatBuffer getVertexBuf() { return mVertexBuf.getBuf(); }
	public FloatBuffer getNormalBuf() { return mNormalBuf.getBuf(); }
	public FloatBuffer getTextureBuf() { return mTextureBuf.getBuf(); }
	
	public int getNumVertexes() { return mVertexBuf.capacity(); } 
	
//	static final protected int FIXED_COLOR_ONE = 0x10000;
//	protected ShortData getColorFixed() { return null; }
	
	public enum CullFace { NONE, BACK, FRONT }
	public Shape setCullFace(CullFace code) { 
		if (code == CullFace.BACK) {
    		mCullFace = GL10.GL_BACK;
		} else if (code == CullFace.FRONT) {
    		mCullFace = GL10.GL_FRONT;
		} else {
    		mCullFace = 0;
		}
		return this;
	}
	
	public Color4f getColor() {
		if (mColor == null) {
			mColor = dGetColor();
		}
		return mColor;
	}
	
	public void setColor(Color4f color) { mColor = color; }
	
	
//	public static CullFace GetCullFaceFromOrdinal(int face) {
//    	for (Shape.CullFace match : Shape.CullFace.values()) {
//    		if (match.ordinal() == face) {
//    			return match;
//    		}
//    	}
//    	return Shape.CullFace.NONE;
//	}

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
						} else if (bone.mJoints != null && boneO.mJoints == null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(": first has joints, second didn't");
							msg.msg(tag, sbuf.toString());
						} else if (bone.mJoints == null && boneO.mJoints != null) {
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
						if (bone.mAnim != null && boneO.mAnim == null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(": first had animations, second didn't");
							msg.msg(tag, sbuf.toString());
						} else if (bone.mAnim == null && boneO.mAnim != null) {
							StringBuffer sbuf = new StringBuffer();
							sbuf.append("Bone ");
							sbuf.append(i);
							sbuf.append(": second had animations, first didn't");
							msg.msg(tag, sbuf.toString());
						} else if (bone.mAnim != null) {
							if (bone.mAnim.size() != boneO.mAnim.size()) {
								StringBuffer sbuf = new StringBuffer();
								sbuf.append("Bone ");
								sbuf.append(i);
								sbuf.append(": first had ");
								sbuf.append(bone.mAnim.size());
								sbuf.append(" animations, second had ");
								sbuf.append(boneO.mAnim.size());
								msg.msg(tag, sbuf.toString());
							} else {
								for (int set_index = 0; set_index < bone.mAnim.size(); set_index++) {
									AnimSet animSet = bone.mAnim.get(set_index);
									AnimSet animSetO = boneO.mAnim.get(set_index);
									if (animSet.mControls.keySet().size() != animSetO.mControls.keySet().size()) {
										StringBuffer sbuf = new StringBuffer();
										sbuf.append("Bone ");
										sbuf.append(i);
										sbuf.append(" #anim ");
										sbuf.append(animSet.mControls.keySet().size());
										sbuf.append(" != ");
										sbuf.append(animSetO.mControls.keySet().size());
										msg.msg(tag, sbuf.toString());
									} else {
										for (AnimType type : animSet.mControls.keySet()) {
											AnimControl anim1 = animSet.mControls.get(type);
											AnimControl anim2 = animSetO.mControls.get(type);
											if (anim2 == null) {
												StringBuffer sbuf = new StringBuffer();
												sbuf.append("Bone2 does not have anim key ");
												sbuf.append(type.toString());
												msg.msg(tag, sbuf.toString());
											} else {
												if (anim1.num() != anim2.num()) {
													StringBuffer sbuf = new StringBuffer();
													sbuf.append("Bone ");
													sbuf.append(i);
													sbuf.append(" #anim ");
													sbuf.append(anim1.num());
													sbuf.append(" != ");
													sbuf.append(anim2.num());
													msg.msg(tag, sbuf.toString());
												} else if (anim1.num() > 0) {
													boolean identical = true;
													for (int j = 0; j < anim1.num(); j++) {
														if (anim1.get(j) != anim2.get(j)) {
															StringBuffer sbuf = new StringBuffer();
															sbuf.append("Bone#");
															sbuf.append(i);
															sbuf.append(", anim pt# ");
															sbuf.append(j);
															sbuf.append(": ");
															sbuf.append(anim1.get(j));
															sbuf.append(" != ");
															sbuf.append(anim2.get(j));
															msg.msg(tag, sbuf.toString());
															identical = false;
														}
													}
													if (identical) {
														StringBuffer sbuf = new StringBuffer();
														sbuf.append("Bone#");
														sbuf.append(i);
														sbuf.append(", ");
														sbuf.append(anim1.num());
														sbuf.append(" anim pts identical");
														msg.msg(tag, sbuf.toString());
													}
												}
											}
										}
									}
								}
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
				if (getChildren().size() != other.getChildren().size()) {
					StringBuffer sbuf = new StringBuffer();
					sbuf.append("First had ");
					sbuf.append(getChildren().size());
					sbuf.append(" children, second had ");
					sbuf.append(other.getChildren().size());
					sbuf.append(" children.");
					msg.msg(tag, sbuf.toString());
				} else {
					for (int i = 0; i < getChildren().size(); i++) {
						Shape child = getChild(i);
						Shape childO = other.getChild(i);
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
		Bounds3D bounds = getBounds();
		Vector3f midPoint = new Vector3f(bounds.getMidX(), bounds.getMidY(), bounds.getMidZ());
		Matrix4f matrix = getMatrix();
		matrix.multMV(midPoint);
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
	
	public boolean hasAnimation() {
		return (getAnimBone() != null);
	}
	
	public FloatBuffer setColorBuf(int size) {
		mColorBuf = new FloatBuf();
		FloatBuffer buf = mColorBuf.alloc(size);
		buf.rewind();
		return buf;
	}
	
	public ShortBuffer setIndexBuf(int size) {
		mIndexBuf = new ShortBuf();
		ShortBuffer buf = mIndexBuf.alloc(size);
		buf.rewind();
		return buf;
	}
	
	public FloatBuffer setNormalBuf(int size) {
		mNormalBuf = new FloatBuf();
		FloatBuffer buf = mNormalBuf.alloc(size);
		buf.rewind();
		return buf;
	}
	
	public FloatBuffer setVertexBuf(int size) {
		mVertexBuf = new FloatBuf();
		FloatBuffer buf = mVertexBuf.alloc(size);
		buf.rewind();
		return buf;
	}
	
	public FloatBuffer setTextureBuf(int size) {
		mTextureBuf = new FloatBuf();
		FloatBuffer buf = mTextureBuf.alloc(size);
		buf.rewind();
		return buf;
	}
	
	///////////////////////////////////////
	// DEFINE
	///////////////////////////////////////
	
	public enum AnimType {
		LOC_X, LOC_Y, LOC_Z,
		SCALE_X, SCALE_Y, SCALE_Z,
		QUAT_X, QUAT_Y, QUAT_Z, QUAT_W,
		ROT_X, ROT_Y, ROT_Z, 
		UNKNOWN;
		
		static AnimType from(int value) {
			for (AnimType type : AnimType.values()) {
				if (type.ordinal() == value) {
					return type;
				}
			}
			return UNKNOWN;
		}
	};
	
	public enum AnimControlOp {
		ControlBoneOnly,
		ControlBoneAndChildren,
		ControlShape,
		UNKNOWN;
		
		static AnimControlOp from(int value) {
			for (AnimControlOp type : AnimControlOp.values()) {
				if (type.ordinal() == value) {
					return type;
				}
			}
			return UNKNOWN;
		}
	};
	
	public static final int NUM_ANIM_TYPES = 10;
	
	public class dBone implements dShortBuf {
		public String getName() { return null; }
		public void fill(ShortBuffer buf) {}
		public int size() { return 0; }
		public int [] getJoints() { return null; }
		public int getJointParent() { return -1; }
		public String [] getAnimSets() { return null; }
		public float [] getAnimKnotPts(int set, AnimType type) { return null; }
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
	
	public Shape fill() {
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
			mChildren = new ArrayList<Shape>(children.length);
			for (Shape child : children) {
				mChildren.add(child);
			}
			for (Shape child : mChildren) {
				child.fill();
			}
		}
		getBounds().compute();

		// It is arguably faster and easier to use floats
		// because modern hardware supports colors floats.
		// If not, then it is faster to use GL_FIXED.
		// Right now, this code is optimized for modern hardware.

		//setColorData(getColorFixed()); // Note: uses FIXED, which means one is 0x10000.
		return this;
	}

	public ArrayList<Shape> resetChildren() {
		mChildren = new ArrayList<Shape>();
		return mChildren;
	}
	
	public void resetChildren(Shape child) {
		resetChildren();
		mChildren.add(child);
	}
	
	public void resetChildren(Shape [] children) {
		resetChildren();
		for (Shape shape : children) {
			mChildren.add(shape);
		}
	}
	
	public void addChild(Shape child) {
		if (mChildren == null) {
			mChildren = new ArrayList<Shape>();
		}
		mChildren.add(child);
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
				tgt.computeBounds();
				
				String [] animSets = src.getAnimSets();
				if (animSets != null) {
					for (int set_index = 0; set_index < animSets.length; set_index++) {
						String animName = animSets[set_index];
						AnimSet set = tgt.allocAnimSet(animName);
    					for (AnimType type : AnimType.values()) {
    						float [] pts = src.getAnimKnotPts(set_index, type);
    						if (pts != null) {
            					set.setAnimKnotPts(type, pts);
    						}
    					}
    					set.computeBounds();
					}
				}
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
	
	public void resetRotate() {
		mMatrixMod = new Matrix4f();
	}

	// Returns the currently active matrix that should be applied for drawing.
	// Warning: this can return NULL.
	protected Matrix4f getMatrix() {
		if (mMatrixMod != null) {
			return mMatrixMod;
		}
		return mMatrix;
	}
	
	// Get the modification matrix that lives on top of the object matrix.
	// Will never return null.
	public Matrix4f getMatrixMod() {
		if (mMatrixMod == null) {
			mMatrixMod = new Matrix4f(mMatrix);
		}
		return mMatrixMod;
	}
	
	public void setMatrixMod(Matrix4f mod) {
		mMatrixMod = mod;
	}
	
	public Quaternion getQuaternionMod() { 
		return getMatrixMod().getQuaternion();
	}
	
	public Vector3f getLocationMod() { 
		return getMatrixMod().getLocation(); 
	}
	
	public void setLocation(Vector3f x) { 
		getMatrixMod().setLocation(x);
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

		if (getChildren() != null && getChildren().size() > 0) {
			dataStream.writeInt(ELE_CHILDREN);
			dataStream.writeInt(getChildren().size());
			for (int i = 0; i < getChildren().size(); i++) {
				getChild(i).writeData(dataStream);
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
		mChildren = new ArrayList<Shape>(numChildren);
		for (int i = 0; i < numChildren; i++) {
			Shape child = new Shape();
			child.readData(dataStream, tm);
			mChildren.add(child);
		}
	}

	// 
	// BOUNDS
	// 
	protected void writeBounds(DataOutputStream dataStream) throws IOException {
		dataStream.writeInt(ELE_BOUNDS);
		getBounds().write(dataStream);
	}

	protected void readBounds(DataInputStream dataStream) throws IOException {
		getBounds().read(dataStream);
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
		bone.getBounds().write(dataStream);
		bone.writeBuffer(dataStream);
		
		if (bone.mAnim != null) {
			dataStream.writeInt(bone.mAnim.size());
			for (AnimSet set : bone.mAnim){ 
				ShapeUtils.writeString(dataStream, set.mName);
				dataStream.writeInt(set.getControlOp().ordinal());
    			dataStream.writeInt(set.mControls.keySet().size());
    			for (AnimType type : set.mControls.keySet()) {
    				AnimControl control = set.mControls.get(type);
        			dataStream.writeInt(type.ordinal());
    				dataStream.writeInt(control.mPairs.length);
    				for (float f : control.mPairs) {
    					dataStream.writeFloat(f);
    				}
    			}
			}
		} else {
			dataStream.writeInt(0);
		}
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
		bone.mBounds = new Bounds3D();
		bone.mBounds.read(dataStream);
		bone.readBuffer(dataStream);
		
		int animSetCount = dataStream.readInt();
		if (animSetCount > 0) {
			for (int set_index = 0; set_index < animSetCount; set_index++) {
				String animName = ShapeUtils.readString(dataStream);
				AnimSet set = bone.allocAnimSet(animName);
				int controlOp = dataStream.readInt();
				set.setControlOp(AnimControlOp.from(controlOp));
				int animControlCount = dataStream.readInt();
				for (int count = 0; count < animControlCount; count++) {
					int typeI = dataStream.readInt();
					AnimType typeA = AnimType.from(typeI);
					int numpts = dataStream.readInt();
					float [] pts = new float[numpts];
					for (int i = 0; i < numpts; i++) {
						pts[i] = dataStream.readFloat();
					}
					set.setAnimKnotPts(typeA, pts);
				}
				set.computeBounds();
			}
		}
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
	// Color Outline override
	///////////////////////////////////////
	static final Color4f [] mColorMapColors = {
		Color4f.RED,
		Color4f.GREEN,
		Color4f.BLUE,
		Color4f.CYAN,
		Color4f.MAGENTA,
		Color4f.ORANGE,
		Color4f.PINK,
		Color4f.GRAY,
		Color4f.BROWN,
		Color4f.BLACK,
		Color4f.YELLOW,
		Color4f.LIGHT_GRAY,
		Color4f.DARK_GRAY,
	};
	
	public class ColorMap {
		HashMap<Integer,Shape> mMap;
		
		int mNextColor = 0;
		
		ColorMap() {
			mMap = new HashMap<Integer,Shape>();
		}
		
		Color4f getNextColor() {
			int baseColor = mNextColor % mColorMapColors.length;
			int offset = mNextColor / mColorMapColors.length;
			
			mNextColor++;
			
			Color4f choice = mColorMapColors[baseColor];
			
			if (offset > 0) {
				int which = (offset-1) % 3;
				int amt = (offset-1)/3+1;
				float color;

				while (which < 3) {
					if (which == 0) {
						color = choice.getRed();
					} else if (which == 1) {
						color = choice.getGreen();
					} else {
						color = choice.getBlue();
					}
					color += amt * 0.1f;

					if (color <= 1) {
						choice = new Color4f(choice);
						
						if (which == 0) {
							choice.setRed(color);
						} else if (which == 1) {
							choice.setGreen(color);
						} else {
							choice.setBlue(color);
						}
						break;
					}
					which++;
				}
				if (which >= 3) {
					return getNextColor();
				}
			}
			return choice;
		}
		
		void assign(Shape shape) {
			shape.mColorOutline = getNextColor();
			int color = shape.mColorOutline.getColor();
			mMap.put(color, shape);
		}
		
		public Shape getShape(int pixel) {
			return mMap.get(pixel);
		}
	};
	
	// Draw the shape as a solid color shape only. No details.
	// Note: children are left as is. Must do each child individually.
	public ColorMap setOutlineOverride() {
		ColorMap colorMap = new ColorMap();
		setOutlineOverride(colorMap);
		return colorMap;
	}
	
	protected void setOutlineOverride(ColorMap colorMap) {
		if (hasVertexArray()) {
			colorMap.assign(this);
		}
		if (mChildren != null) {
    		for (Shape child : mChildren) {
    			child.setOutlineOverride(colorMap);
    		}
		}
	}
	
	public void clearOutlineOverride() {
		mColorOutline = null;
		
		if (mChildren != null) {
    		for (Shape child : mChildren) {
    			child.clearOutlineOverride();
    		}
		}
	}
	
	public boolean hasOutlineOverride() {
		return mColorOutline != null;
	}

	///////////////////////////////////////
	// OpenGL
	///////////////////////////////////////

	public void onDraw(MatrixTrackingGL gl) {
		FloatBuffer fbuf;
		boolean didPush = false;
		
		if (mColorOutline != null) {
			gl.glColor4f(mColorOutline.getRed(), mColorOutline.getGreen(), mColorOutline.getBlue(), mColorOutline.getAlpha());
		} else if (!hasColorArray()) {
			Color4f color = getColor();
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
		if (mColorOutline != null) {
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		} else {
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
    			// gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuf.asShortBuffer());
    		} else {
    			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    		}
    		if ((mTexture != null) && ((fbuf = getTextureBuf()) != null)) {
    			mTexture.onDrawOld(gl, fbuf);
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

//	public void onFinished(MatrixTrackingGL gl) {
//		if (mChildren != null) {
//			for (Shape shape : mChildren) {
//				shape.onFinished(gl);
//			}
//		}
//	}
}
