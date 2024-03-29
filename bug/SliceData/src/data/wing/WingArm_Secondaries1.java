/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.dugsolutions.jacket.math.Matrix4f;
import com.dugsolutions.jacket.shape.Shape;
import com.dugsolutions.jacket.shape.BufferUtils.dFloatBuf;
import com.dugsolutions.jacket.shape.BufferUtils.dShortBuf;


public class WingArm_Secondaries1 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(-0.254999f, 0.000000f, 0.966941f, -1.067066f,
		                    0.000000f, 1.000000f, -0.000000f, -0.990229f,
		                    -0.966941f, -0.000000f, -0.254999f, -0.814086f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.000000f; }
	@Override protected float dGetMaxX() { return 1.024142f; }
	@Override protected float dGetMinY() { return 0.996523f; }
	@Override protected float dGetMaxY() { return 1.015581f; }
	@Override protected float dGetMinZ() { return -0.500000f; }
	@Override protected float dGetMaxZ() { return 0.778408f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					1.000000f, 1.015581f, -0.500000f, 1.024142f, 0.996523f, 0.778408f, -0.975858f, 0.996523f, 0.778407f, -1.000000f, 1.015581f, -0.500000f, /* 0 -> 3 */
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
					0.000000f, 0.999878f, 0.014893f, 0.000000f, 0.999878f, 0.014893f, 0.000000f, 0.999878f, 0.014893f, 0.000000f, 0.999878f, 0.014893f,  /* 0 -> 3 */
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
