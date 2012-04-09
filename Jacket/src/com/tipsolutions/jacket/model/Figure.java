package com.tipsolutions.jacket.model;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.tipsolutions.jacket.math.BufferUtils.Bounds;
import com.tipsolutions.jacket.math.BufferUtils.ComputeBounds;
import com.tipsolutions.jacket.math.BufferUtils.ShortBuf;
import com.tipsolutions.jacket.math.BufferUtils.ShortBufSortedRange;
import com.tipsolutions.jacket.math.Point;

public class Figure extends Model {

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
	
	public class AnimSet {
		public class AnimControl {
			float [] mPairs;

			public AnimControl(float [] pairs) {
				mPairs = pairs;
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

			public float get(int i) {
				return mPairs[i];
			}

			float getFirstTime() {
				return mPairs[0];
			}

			float getLastTime() {
				return mPairs[mPairs.length-2];
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

			public int num() {
				return mPairs.length;
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

		public void cullToTime(float startTime, float endTime) {
			for (AnimType type : mControls.keySet()) {
				mControls.get(type).cullToTime(startTime, endTime);
			}
			computeBounds();
		}

		public AnimControl getAnim(AnimType key) {
			return mControls.get(key);
		}

		public Set<AnimType> getControlKeys() {
			return mControls.keySet();
		}

		public AnimControlOp getControlOp() {
			return mControlOp;
		}

		public float getEndTime() {
			return mAnimEndTime;
		}

		public int getNumControls() {
			if (mControls == null) {
				return 0;
			}
			return mControls.keySet().size();
		}

		public float getStartTime() {
			return mAnimStartTime;
		}

		public void setAnimKnotPts(AnimType type, float [] pts) {
			mControls.put(type,new AnimControl(pts));
		}

		public void setControlOp(AnimControlOp op) {
			mControlOp = op;
		}
	};
	
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

	public class Bone extends ShortBufSortedRange {
		protected String mName;
		protected int [] mJoints = null;
		protected int mJointParent = -1;
		protected Bounds mBounds = null;
		protected ArrayList<AnimSet> mAnim = null;

		public AnimSet allocAnimSet(String name) {
			AnimSet set = new AnimSet(name);
			if (mAnim == null) {
				mAnim = new ArrayList<AnimSet>();
			}
			mAnim.add(set);
			return set;
		}

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
			mBounds = new Bounds();
			mBounds.set(computeBounds);
		}

		public ArrayList<AnimSet> getAnimSet() {
			return mAnim;
		}

		public AnimSet getAnimSet(int index) {
			if ((mAnim == null) || (index < 0) || (index >= mAnim.size())) {
				return null;
			}
			return mAnim.get(index);
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

		public ArrayList<AnimSet> getAnimSets() {
			return mAnim;
		}

		public Bounds getBounds() { return mBounds; }

		public Joint getJoint(int index) {
			if (index >= 0 && index < mJoints.length) {
				return Figure.this.getJoint(mJoints[index]);
			}
			return null;
		}

		public Joint getJointParent() {
			return Figure.this.getJoint(mJointParent);
		}

		public String getName() {
			return mName;
		}

		public int getNumAnim() {
			if (mAnim == null) {
				return 0;
			}
			return mAnim.size();
		}

		public int getNumJoints() {
			return mJoints.length;
		}

	};

	public class Joint extends ShortBuf {
		protected int [] mBones = null;

		public Bone getBone(int index) {
			if (index >=0 && index < mBones.length) {
				return Figure.this.getBone(mBones[index]);
			}
			return null;
		}

		public int getNumBones() {
			return mBones.length;
		}
	};

	protected Bone [] mBones;
	protected Joint [] mJoints;

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
	public Bone getBone(int index) {
		if (index >= 0 && index < mBones.length) {
			return mBones[index];
		}
		return null;
	}
	
	public Bone [] getBones() { return mBones; }

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

	public Joint getJoint(int index) {
		if (index >= 0 && index < mJoints.length) {
			return mJoints[index];
		}
		return null;
	}

	public Joint [] getJoints() { return mJoints; }

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
}

