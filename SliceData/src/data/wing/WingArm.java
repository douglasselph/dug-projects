/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.shape.Shape;


public class WingArm extends Shape {
	@Override protected Shape [] dGetChildren() {
		Shape [] children = new Shape[7];
		children[0] = new WingArm_Primaries1();
		children[1] = new WingArm_Primaries2();
		children[2] = new WingArm_Primaries3();
		children[3] = new WingArm_Secondaries1();
		children[4] = new WingArm_Secondaries2();
		children[5] = new WingArm_Secondaries3();
		children[6] = new WingArm_Secondaries4();
		return children;
	}

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(-0.009661f, -0.000000f, -1.592508f, 0.466308f,
		                    0.000000f, 1.592538f, -0.000000f, -0.031165f,
		                    1.592508f, -0.000000f, -0.009661f, 1.889388f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.334232f; }
	@Override protected float dGetMaxX() { return 2.494289f; }
	@Override protected float dGetMinY() { return -0.028067f; }
	@Override protected float dGetMaxY() { return 0.073363f; }
	@Override protected float dGetMinZ() { return -0.183519f; }
	@Override protected float dGetMaxZ() { return 0.514780f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.133726f, 0.060180f, -0.183519f, 0.133726f, -0.014884f, -0.183519f, -1.334232f, -0.028067f, 0.211233f, -1.334232f, 0.073363f, 0.211233f, 0.133726f, 0.060180f, 0.012227f, /* 0 -> 4 */
					0.133725f, -0.014884f, 0.012227f, -1.334232f, -0.028067f, 0.475732f, -1.334232f, 0.073363f, 0.475732f, 1.767368f, -0.003589f, 0.514780f, 1.767368f, 0.048884f, 0.514780f, /* 5 -> 9 */
					1.767368f, -0.003589f, 0.377946f, 1.767368f, 0.048884f, 0.377946f, 2.494289f, 0.032693f, 0.334952f, 2.494289f, 0.012602f, 0.334952f, 2.494288f, 0.032693f, 0.387343f, /* 10 -> 14 */
					2.494288f, 0.012602f, 0.387343f, /* 15 -> 15 */
				});
			};
			public int size() { return 48; }

		};
		return new VertexData();
	};

	@Override
	protected dFloatBuf dGetNormalDef() {
		class NormalData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					-0.050844f, 0.570025f, -0.820032f, 0.115696f, -0.569597f, -0.813715f, -0.668813f, -0.534745f, -0.516404f, -0.487991f, 0.785974f, -0.379528f, -0.051790f, 0.811853f, 0.581530f,  /* 0 -> 4 */
					0.068941f, -0.811212f, 0.580645f, -0.446699f, -0.647450f, 0.617420f, -0.541978f, 0.390210f, 0.744255f, -0.109043f, -0.564745f, 0.818018f, 0.022706f, 0.564104f, 0.825343f,  /* 5 -> 9 */
					0.053346f, -0.804346f, -0.591723f, 0.130467f, 0.803613f, -0.580615f, 0.648885f, 0.340678f, -0.680349f, 0.563311f, -0.584674f, -0.583789f, 0.479263f, 0.787286f, 0.387829f,  /* 10 -> 14 */
					0.648274f, -0.542375f, 0.534349f,  /* 15 -> 15 */
				});
			};

			public int size() { return 48; }

		};
		return new NormalData();
	};

	@Override
	protected dShortBuf dGetIndexDef() {
		class IndexData implements dShortBuf {
			public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					12,14,15,12,15,13,9,8,15,9,15,14,8,10,15,10,13,15,11,9,14,11,14,12,10,11,12,10,12,13,/* 0 -> 9 */
					1,0,11,1,11,10,0,4,11,4,9,11,5,1,10,5,10,8,4,5,8,4,8,9,4,0,3,4,3,7,/* 10 -> 19 */
					2,6,7,2,7,3,1,5,2,5,6,2,4,7,5,7,6,5,0,1,2,0,2,3,/* 20 -> 27 */
				});
			};
			public int size() { return 84; }

		};
		return new IndexData();
	};

	@Override
	protected dBone [] dGetBonesDef() {
		dBone [] bones = new dBone[3];

		bones[0] = new dBone() {
			@Override public String getName() { return "Arm1"; }
			@Override public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					7,0,});
			}
			@Override public int size() { return 2; }
			@Override public int [] getJoints() {
				int [] joints = new int[1];
				joints[0] = 0;
				return joints;
			};
			@Override public String [] getAnimSets() {
				return new String [] {
					"ActIpo",
				};
			}
			@Override public float [] getAnimKnotPts(int set, AnimType type) {
				if (set == 0) {
					if (type == AnimType.QUAT_X) {
						return new float [] {
							0.040000f, 0.000000f,
							0.240000f, 0.017682f,
							0.600000f, -0.021447f,
							0.960000f, -0.000009f,
							10.000000f, 0.000000f,
							10.440000f, 0.014603f,
						};
					}
					if (type == AnimType.QUAT_Y) {
						return new float [] {
							0.040000f, 0.000000f,
							0.240000f, -0.039845f,
							0.600000f, 0.048329f,
							0.960000f, 0.000020f,
							10.000000f, 0.000000f,
							10.440000f, -0.032907f,
						};
					}
					if (type == AnimType.QUAT_Z) {
						return new float [] {
							0.040000f, 0.000000f,
							0.240000f, 0.275492f,
							0.600000f, -0.334156f,
							0.960000f, -0.000136f,
							10.000000f, 0.000000f,
							10.440000f, 0.227523f,
						};
					}
					if (type == AnimType.QUAT_W) {
						return new float [] {
							0.040000f, 1.000000f,
							0.240000f, 0.960315f,
							0.600000f, 0.941033f,
							0.960000f, 1.000000f,
							10.000000f, 1.000000f,
							10.440000f, 0.973107f,
						};
					}
				}
				return null;
			}
		};
		bones[1] = new dBone() {
			@Override public String getName() { return "Arm2"; }
			@Override public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					0,1,4,5,11,8,});
			}
			@Override public int size() { return 6; }
			@Override public int [] getJoints() {
				int [] joints = new int[1];
				joints[0] = 1;
				return joints;
			};
			@Override public int getJointParent() { return 0; }
		};
		bones[2] = new dBone() {
			@Override public String getName() { return "Hand"; }
			@Override public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					15,8,});
			}
			@Override public int size() { return 2; }
			@Override public int [] getJoints() {
				return null;
			};
			@Override public int getJointParent() { return 1; }
		};
		return bones;
	};

	@Override
	protected dJoint [] dGetJointsDef() {
		dJoint [] joints = new dJoint[2];
		joints[0] = new dJoint() {
			@Override public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					0,1,4,5,});
			}
			@Override public int size() { return 4; }
			@Override public int [] getBones() {
				int [] bones = new int[2];
				bones[0] = 0;
				bones[1] = 1;
				return bones;
			};
		};
		joints[1] = new dJoint() {
			@Override public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					11,8,});
			}
			@Override public int size() { return 2; }
			@Override public int [] getBones() {
				int [] bones = new int[2];
				bones[0] = 1;
				bones[1] = 2;
				return bones;
			};
		};
		return joints;
	};

};
