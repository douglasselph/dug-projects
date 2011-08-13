/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.ShapeData;
import com.tipsolutions.jacket.math.Matrix4f;


class Cube extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 1.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -1.000001f; }
	@Override protected float _getMaxY() { return 1.000000f; }
	@Override protected float _getMinZ() { return -1.000000f; }
	@Override protected float _getMaxZ() { return 1.000000f; }

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
				buf.put(-1.000000f).put(1.000000f).put(1.000000f); /* 8 */
				buf.put(-1.000000f).put(1.000000f).put(-1.000000f); /* 9 */
				buf.put(-1.000000f).put(-1.000000f).put(-1.000000f); /* 10 */
				buf.put(0.999999f).put(-1.000001f).put(1.000000f); /* 11 */
				buf.put(-1.000000f).put(-1.000000f).put(1.000000f); /* 12 */
				buf.put(-1.000000f).put(-1.000000f).put(-1.000000f); /* 13 */
				buf.put(1.000000f).put(1.000000f).put(-1.000000f); /* 14 */
				buf.put(1.000000f).put(0.999999f).put(1.000000f); /* 15 */
				buf.put(1.000000f).put(-1.000000f).put(-1.000000f); /* 16 */
				buf.put(0.999999f).put(-1.000001f).put(1.000000f); /* 17 */
				buf.put(1.000000f).put(0.999999f).put(1.000000f); /* 18 */
				buf.put(-1.000000f).put(1.000000f).put(1.000000f); /* 19 */
				buf.put(0.999999f).put(-1.000001f).put(1.000000f); /* 20 */
				buf.put(-1.000000f).put(1.000000f).put(1.000000f); /* 21 */
				buf.put(-1.000000f).put(-1.000000f).put(1.000000f); /* 22 */
				buf.put(0.999999f).put(-1.000001f).put(1.000000f); /* 23 */
				buf.put(1.000000f).put(1.000000f).put(-1.000000f); /* 24 */
				buf.put(1.000000f).put(-1.000000f).put(-1.000000f); /* 25 */
				buf.put(-1.000000f).put(-1.000000f).put(-1.000000f); /* 26 */
				buf.put(1.000000f).put(1.000000f).put(-1.000000f); /* 27 */
				buf.put(-1.000000f).put(-1.000000f).put(-1.000000f); /* 28 */
				buf.put(-1.000000f).put(1.000000f).put(-1.000000f); /* 29 */
			};
			public int size() { return 90; }

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
				buf.put(-0.666646f).put(0.333323f).put(0.666646f); /* 8 */
				buf.put(-0.408246f).put(0.816492f).put(-0.408246f); /* 9 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 10 */
				buf.put(0.333323f).put(-0.666646f).put(0.666646f); /* 11 */
				buf.put(-0.577349f).put(-0.577349f).put(0.577349f); /* 12 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 13 */
				buf.put(0.408246f).put(0.408246f).put(-0.816492f); /* 14 */
				buf.put(0.666646f).put(0.666646f).put(0.333323f); /* 15 */
				buf.put(0.816492f).put(-0.408246f).put(-0.408246f); /* 16 */
				buf.put(0.333323f).put(-0.666646f).put(0.666646f); /* 17 */
				buf.put(0.666646f).put(0.666646f).put(0.333323f); /* 18 */
				buf.put(-0.666646f).put(0.333323f).put(0.666646f); /* 19 */
				buf.put(0.333323f).put(-0.666646f).put(0.666646f); /* 20 */
				buf.put(-0.666646f).put(0.333323f).put(0.666646f); /* 21 */
				buf.put(-0.577349f).put(-0.577349f).put(0.577349f); /* 22 */
				buf.put(0.333323f).put(-0.666646f).put(0.666646f); /* 23 */
				buf.put(0.408246f).put(0.408246f).put(-0.816492f); /* 24 */
				buf.put(0.816492f).put(-0.408246f).put(-0.408246f); /* 25 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 26 */
				buf.put(0.408246f).put(0.408246f).put(-0.816492f); /* 27 */
				buf.put(-0.577349f).put(-0.577349f).put(-0.577349f); /* 28 */
				buf.put(-0.408246f).put(0.816492f).put(-0.408246f); /* 29 */
			};

			public int size() { return 90; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)4).put((short)0).put((short)3); /* 0 */
				buf.put((short)4).put((short)3).put((short)7); /* 1 */
				buf.put((short)2).put((short)6).put((short)8); /* 2 */
				buf.put((short)2).put((short)8).put((short)9); /* 3 */
				buf.put((short)1).put((short)5).put((short)10); /* 4 */
				buf.put((short)11).put((short)12).put((short)13); /* 5 */
				buf.put((short)14).put((short)15).put((short)16); /* 6 */
				buf.put((short)15).put((short)17).put((short)16); /* 7 */
				buf.put((short)18).put((short)19).put((short)20); /* 8 */
				buf.put((short)21).put((short)22).put((short)23); /* 9 */
				buf.put((short)24).put((short)25).put((short)26); /* 10 */
				buf.put((short)27).put((short)28).put((short)29); /* 11 */
			};
			public int size() { return 36; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "CubePaint.png"; }

	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.826603f).put(0.972696f); /* 0 */
				buf.put(0.827490f).put(0.474609f); /* 1 */
				buf.put(0.069757f).put(0.729862f); /* 2 */
				buf.put(0.582447f).put(0.971355f); /* 3 */
				buf.put(0.825087f).put(0.728715f); /* 4 */
				buf.put(0.827490f).put(0.717250f); /* 5 */
				buf.put(0.069050f).put(0.486286f); /* 6 */
				buf.put(0.580931f).put(0.727373f); /* 7 */
				buf.put(0.312397f).put(0.487221f); /* 8 */
				buf.put(0.313104f).put(0.730797f); /* 9 */
				buf.put(0.584849f).put(0.474609f); /* 10 */
				buf.put(0.827220f).put(0.719780f); /* 11 */
				buf.put(0.584579f).put(0.719780f); /* 12 */
				buf.put(0.584579f).put(0.477139f); /* 13 */
				buf.put(0.567374f).put(0.481001f); /* 14 */
				buf.put(0.567031f).put(0.723945f); /* 15 */
				buf.put(0.324391f).put(0.481304f); /* 16 */
				buf.put(0.324048f).put(0.724248f); /* 17 */
				buf.put(0.270296f).put(0.990269f); /* 18 */
				buf.put(0.027655f).put(0.990269f); /* 19 */
				buf.put(0.270296f).put(0.747629f); /* 20 */
				buf.put(0.020625f).put(0.989047f); /* 21 */
				buf.put(0.020625f).put(0.746406f); /* 22 */
				buf.put(0.263266f).put(0.746406f); /* 23 */
				buf.put(0.559062f).put(0.735953f); /* 24 */
				buf.put(0.559062f).put(0.978594f); /* 25 */
				buf.put(0.316422f).put(0.978594f); /* 26 */
				buf.put(0.555745f).put(0.730797f); /* 27 */
				buf.put(0.313104f).put(0.973437f); /* 28 */
				buf.put(0.313104f).put(0.730797f); /* 29 */
			};
			public int size() { return 60; }

		};
		return new TextureData();
	};

};
