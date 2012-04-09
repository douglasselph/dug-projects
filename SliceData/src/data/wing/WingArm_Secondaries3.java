/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.shape.Shape;


public class WingArm_Secondaries3 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.015643f, 0.000000f, 0.999878f, 0.410449f,
		                    0.000000f, 1.000000f, -0.000000f, -0.990229f,
		                    -0.999878f, 0.000000f, 0.015643f, -1.195696f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.164668f; }
	@Override protected float dGetMaxX() { return 0.933045f; }
	@Override protected float dGetMinY() { return 0.996523f; }
	@Override protected float dGetMaxY() { return 1.015581f; }
	@Override protected float dGetMinZ() { return -0.401092f; }
	@Override protected float dGetMaxZ() { return 0.525869f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.933045f, 1.015581f, -0.401092f, 0.902758f, 0.996523f, 0.525869f, -1.164668f, 0.996523f, -0.030882f, -1.056406f, 1.015581f, -0.352327f, /* 0 -> 3 */
				});
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected dFloatBuf dGetNormalDef() {
		class NormalData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					-0.001984f, 0.999176f, 0.040010f, -0.005463f, 0.999756f, 0.020356f, -0.001984f, 0.999176f, 0.040010f, 0.001434f, 0.998199f, 0.059664f,  /* 0 -> 3 */
				});
			};

			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected dShortBuf dGetIndexDef() {
		class IndexData implements dShortBuf {
			public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					0,3,2,0,2,1,/* 0 -> 1 */
				});
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

};
