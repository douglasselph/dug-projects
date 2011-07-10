/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.ShapeData;
class cube extends ShapeData {

	@Override public float _getMinX() { return -1.000000f; }
	@Override public float _getMaxX() { return 1.000000f; }
	@Override public float _getMinY() { return -1.000001f; }
	@Override public float _getMaxY() { return 1.000000f; }
	@Override public float _getMinZ() { return -1.000000f; }
	@Override public float _getMaxZ() { return 1.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f).put(-1.000000f); /* 0 */
				buf.put(1.000000f).put(-1.000000f).put(-1.000000f); /* 1 */
				buf.put(-1.000000f).put(-1.000000f).put(-1.000000f); /* 2 */
				buf.put(-1.000000f).put(1.000000f).put(-1.000000f); /* 3 */
				buf.put(1.000000f).put(0.999999f).put(1.000000f); /* 4 */
				buf.put(0.999999f).put(-1.000001f).put(1.000000f); /* 5 */
				buf.put(-1.000000f).put(-1.000000f).put(1.000000f); /* 6 */
				buf.put(-1.000000f).put(1.000000f).put(1.000000f); /* 7 */
			};
			public int size() { return 24; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.408246f).put(0.408246f).put(-0.816492f); /* 0 */
				buf.put(0.816492f).put(-0.408246f).put(-0.408246f); /* 1 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 2 */
				buf.put(-0.408246f).put(0.816492f).put(-0.408246f); /* 3 */
				buf.put(0.666646f).put(0.666646f).put(0.333323f); /* 4 */
				buf.put(0.333323f).put(-0.666646f).put(0.666646f); /* 5 */
				buf.put(-0.577349f).put(-0.577349f).put(0.577349f); /* 6 */
				buf.put(-0.666646f).put(0.333323f).put(0.666646f); /* 7 */
			};
			public int size() { return 24; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)4).put((short)0).put((short)3); /* 0 */
				buf.put((short)4).put((short)3).put((short)7); /* 1 */
				buf.put((short)2).put((short)6).put((short)7); /* 2 */
				buf.put((short)2).put((short)7).put((short)3); /* 3 */
				buf.put((short)1).put((short)5).put((short)2); /* 4 */
				buf.put((short)5).put((short)6).put((short)2); /* 5 */
				buf.put((short)0).put((short)4).put((short)1); /* 6 */
				buf.put((short)4).put((short)5).put((short)1); /* 7 */
				buf.put((short)4).put((short)7).put((short)5); /* 8 */
				buf.put((short)7).put((short)6).put((short)5); /* 9 */
				buf.put((short)0).put((short)1).put((short)2); /* 10 */
				buf.put((short)0).put((short)2).put((short)3); /* 11 */
			};
			public int size() { return 36; }

		};
		return new IndexData();
	};

};
