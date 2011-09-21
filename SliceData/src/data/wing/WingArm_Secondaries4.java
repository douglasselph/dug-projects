/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.shape.Shape;
import com.tipsolutions.jacket.math.Matrix4f;


public class WingArm_Secondaries4 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.345688f, -0.013387f, 0.938254f, 1.118676f,
		                    -0.001386f, 0.999890f, 0.014778f, -1.010397f,
		                    -0.938348f, -0.006409f, 0.345632f, -0.870807f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.002912f; }
	@Override protected float dGetMaxX() { return 1.048809f; }
	@Override protected float dGetMinY() { return 1.005236f; }
	@Override protected float dGetMaxY() { return 1.017455f; }
	@Override protected float dGetMinZ() { return -0.622244f; }
	@Override protected float dGetMaxZ() { return 0.999655f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					1.048809f, 1.017455f, -0.622244f, 0.997088f, 1.005236f, 0.999655f, -1.002912f, 1.005236f, 0.999655f, -1.000000f, 1.015581f, -0.500000f, /* 0 -> 3 */
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
					-0.000458f, 0.999969f, 0.007508f, -0.000214f, 0.999969f, 0.007202f, 0.000000f, 0.999969f, 0.006897f, -0.000214f, 0.999969f, 0.007202f,  /* 0 -> 3 */
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
					0,3,1,3,2,1,/* 0 -> 1 */
				});
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

};
