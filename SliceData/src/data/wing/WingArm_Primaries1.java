/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.shape.Shape;


public class WingArm_Primaries1 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.345688f, -0.013387f, 0.938254f, 2.517912f,
		                    -0.001386f, 0.999890f, 0.014778f, -1.010397f,
		                    -0.938348f, -0.006409f, 0.345632f, -0.391349f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.035275f; }
	@Override protected float dGetMaxX() { return 0.999045f; }
	@Override protected float dGetMinY() { return 1.016110f; }
	@Override protected float dGetMaxY() { return 1.024883f; }
	@Override protected float dGetMinZ() { return -0.776386f; }
	@Override protected float dGetMaxZ() { return -0.325245f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.929452f, 1.019568f, -0.776386f, 0.999045f, 1.024820f, -0.325245f, -0.991774f, 1.024883f, -0.328626f, -1.035275f, 1.016110f, -0.539140f, /* 0 -> 3 */
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
					-0.003265f, 0.999634f, -0.025941f, 0.000031f, 0.999908f, -0.011628f, -0.003265f, 0.999634f, -0.025941f, -0.006592f, 0.999146f, -0.040254f,  /* 0 -> 3 */
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
