/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.data.FigureData;

class cube extends FigureData {

	@Override public float getMinX() { return -1.000000f; }
	@Override public float getMaxX() { return 1.000000f; }
	@Override public float getMinY() { return -1.000001f; }
	@Override public float getMaxY() { return 1.000000f; }
	@Override public float getMinZ() { return -1.000000f; }
	@Override public float getMaxZ() { return 1.000000f; }

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
				buf.put(0.577349f).put(0.577349f).put(-0.577349f); /* 0 */
				buf.put(0.577349f).put(-0.577349f).put(-0.577349f); /* 1 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 2 */
				buf.put(-0.577349f).put(0.577349f).put(-0.577349f); /* 3 */
				buf.put(0.577349f).put(0.577349f).put(0.577349f); /* 4 */
				buf.put(0.577349f).put(-0.577349f).put(0.577349f); /* 5 */
				buf.put(-0.577349f).put(-0.577349f).put(0.577349f); /* 6 */
				buf.put(-0.577349f).put(0.577349f).put(0.577349f); /* 7 */
			};
			public int size() { return 24; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)1).put((short)2).put((short)3); /* 0 */
				buf.put((short)4).put((short)7).put((short)6).put((short)5); /* 1 */
				buf.put((short)0).put((short)4).put((short)5).put((short)1); /* 2 */
				buf.put((short)1).put((short)5).put((short)6).put((short)2); /* 3 */
				buf.put((short)2).put((short)6).put((short)7).put((short)3); /* 4 */
				buf.put((short)4).put((short)0).put((short)3).put((short)7); /* 5 */
			};
			public int size() { return 24; }

		};
		return new IndexData();
	};

};
