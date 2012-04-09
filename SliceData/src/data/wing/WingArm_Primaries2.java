/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.math.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.math.BufferUtils.dShortBuf;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.shape.Shape;


public class WingArm_Primaries2 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.345688f, -0.013387f, 0.938254f, 2.684255f,
		                    -0.001386f, 0.999890f, 0.014778f, -1.010397f,
		                    -0.938348f, -0.006409f, 0.345632f, -0.391349f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.092782f; }
	@Override protected float dGetMaxX() { return 1.111627f; }
	@Override protected float dGetMinY() { return 1.014186f; }
	@Override protected float dGetMaxY() { return 1.023436f; }
	@Override protected float dGetMinZ() { return -0.635780f; }
	@Override protected float dGetMaxZ() { return 0.404856f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.887408f, 1.017432f, -0.635780f, 1.111627f, 1.014186f, 0.404856f, -0.932820f, 1.023436f, -0.225222f, -1.092782f, 1.015409f, -0.497105f, /* 0 -> 3 */
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
					0.000397f, 0.999908f, -0.012726f, 0.003815f, 0.999969f, 0.002289f, 0.000397f, 0.999908f, -0.012726f, -0.002960f, 0.999603f, -0.027741f,  /* 0 -> 3 */
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
