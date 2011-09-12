/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.math.Matrix4f;


public class WingArm_Primaries3 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.345688f, -0.013387f, 0.938254f, 3.114789f,
		                    -0.001386f, 0.999890f, 0.014778f, -0.993380f,
		                    -0.938348f, -0.006409f, 0.345632f, -0.459843f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.149319f; }
	@Override protected float dGetMaxX() { return 0.868068f; }
	@Override protected float dGetMinY() { return 0.997884f; }
	@Override protected float dGetMaxY() { return 1.024359f; }
	@Override protected float dGetMinZ() { return -0.622252f; }
	@Override protected float dGetMaxZ() { return 1.425798f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.868068f, 1.008978f, -0.065619f, 0.236461f, 0.997884f, 1.425798f, -0.978244f, 1.024359f, -0.291904f, -1.149319f, 1.017180f, -0.622252f, /* 0 -> 3 */
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
					0.009369f, 0.999908f, -0.008667f, 0.007050f, 0.999908f, 0.010407f, 0.009369f, 0.999908f, -0.008667f, 0.011719f, 0.999542f, -0.027772f,  /* 0 -> 3 */
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
