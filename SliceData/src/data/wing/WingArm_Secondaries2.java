/* THIS IS A GENERATED FILE */

package data.wing;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.dugsolutions.jacket.math.Matrix4f;
import com.dugsolutions.jacket.shape.Shape;
import com.dugsolutions.jacket.shape.BufferUtils.dFloatBuf;
import com.dugsolutions.jacket.shape.BufferUtils.dShortBuf;


public class WingArm_Secondaries2 extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(0.015643f, 0.000000f, 0.999878f, -0.157074f,
		                    0.000000f, 1.000000f, -0.000000f, -0.990229f,
		                    -0.999878f, 0.000000f, 0.015643f, -1.176126f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.141551f; }
	@Override protected float dGetMaxX() { return 1.017866f; }
	@Override protected float dGetMinY() { return 0.996523f; }
	@Override protected float dGetMaxY() { return 1.015581f; }
	@Override protected float dGetMinZ() { return -0.527667f; }
	@Override protected float dGetMaxZ() { return 0.377277f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.891920f, 1.015581f, -0.527667f, 1.017866f, 0.996523f, 0.377277f, -1.042826f, 0.996523f, 0.251008f, -1.141551f, 1.015581f, -0.165059f, /* 0 -> 3 */
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
					0.003235f, 0.999451f, 0.032563f, -0.001282f, 0.999756f, 0.021210f, 0.003235f, 0.999451f, 0.032563f, 0.007813f, 0.998993f, 0.043886f,  /* 0 -> 3 */
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
