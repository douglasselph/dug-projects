package data;
/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.math.Matrix4f;


public class Cube extends Shape {

	@Override protected Matrix4f dGetMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 1.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float dGetMinX() { return -1.000000f; }
	@Override protected float dGetMaxX() { return 1.000000f; }
	@Override protected float dGetMinY() { return -1.000001f; }
	@Override protected float dGetMaxY() { return 1.000000f; }
	@Override protected float dGetMinZ() { return -1.000000f; }
	@Override protected float dGetMaxZ() { return 1.000000f; }

	@Override
	protected dFloatBuf dGetVertexDef() {
		class VertexData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					1.000000f, 1.000000f, -1.000000f, 1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, 1.000000f, -1.000000f, 1.000000f, 0.999999f, 1.000000f, /* 0 -> 4 */
					0.999999f, -1.000001f, 1.000000f, -1.000000f, -1.000000f, 1.000000f, -1.000000f, 1.000000f, 1.000000f, -1.000000f, 1.000000f, 1.000000f, -1.000000f, 1.000000f, -1.000000f, /* 5 -> 9 */
					-1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, 1.000000f, 1.000000f, 1.000000f, -1.000000f, 1.000000f, 0.999999f, 1.000000f, 1.000000f, -1.000000f, -1.000000f, /* 10 -> 14 */
					0.999999f, -1.000001f, 1.000000f, 1.000000f, 0.999999f, 1.000000f, -1.000000f, 1.000000f, 1.000000f, 0.999999f, -1.000001f, 1.000000f, -1.000000f, -1.000000f, 1.000000f, /* 15 -> 19 */
					1.000000f, 1.000000f, -1.000000f, 1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, -1.000000f, 1.000000f, -1.000000f, /* 20 -> 23 */
				});
			};
			public int size() { return 72; }

		};
		return new VertexData();
	};

	@Override
	protected dFloatBuf dGetNormalDef() {
		class NormalData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.408246f, 0.408246f, -0.816492f, 0.816492f, -0.408246f, -0.408246f, -0.577349f, -0.577349f, -0.577349f, -0.408246f, 0.816492f, -0.408246f, 0.666646f, 0.666646f, 0.333323f,  /* 0 -> 4 */
					0.333323f, -0.666646f, 0.666646f, -0.577349f, -0.577349f, 0.577349f, -0.666646f, 0.333323f, 0.666646f, -0.666646f, 0.333323f, 0.666646f, -0.408246f, 0.816492f, -0.408246f,  /* 5 -> 9 */
					-0.577349f, -0.577349f, -0.577349f, -0.577349f, -0.577349f, 0.577349f, 0.408246f, 0.408246f, -0.816492f, 0.666646f, 0.666646f, 0.333323f, 0.816492f, -0.408246f, -0.408246f,  /* 10 -> 14 */
					0.333323f, -0.666646f, 0.666646f, 0.666646f, 0.666646f, 0.333323f, -0.666646f, 0.333323f, 0.666646f, 0.333323f, -0.666646f, 0.666646f, -0.577349f, -0.577349f, 0.577349f,  /* 15 -> 19 */
					0.408246f, 0.408246f, -0.816492f, 0.816492f, -0.408246f, -0.408246f, -0.577349f, -0.577349f, -0.577349f, -0.408246f, 0.816492f, -0.408246f,  /* 20 -> 23 */
				});
			};

			public int size() { return 72; }

		};
		return new NormalData();
	};

	@Override
	protected dShortBuf dGetIndexDef() {
		class IndexData implements dShortBuf {
			public void fill(ShortBuffer buf) {
				buf.put(new short [] {
					4,0,3,4,3,7,2,6,8,2,8,9,1,5,10,5,11,10,12,13,14,13,15,14,16,17,18,17,19,18,/* 0 -> 9 */
					20,21,22,20,22,23,/* 10 -> 11 */
				});
			};
			public int size() { return 36; }

		};
		return new IndexData();
	};

	@Override
	protected String dGetTextureFilename() { return "CubePaint.png"; }

	@Override
	protected dFloatBuf dGetTextureDef() {
		class TextureData implements dFloatBuf {
			public void fill(FloatBuffer buf) {
				buf.put(new float [] {
					0.826603f, 0.972696f, 0.827490f, 0.474609f, 0.069757f, 0.729862f, 0.582447f, 0.971355f, 0.825087f, 0.728715f, 0.827355f, 0.718515f, 0.069050f, 0.486286f, 0.580931f, 0.727373f, 0.312397f, 0.487221f, 0.313104f, 0.730797f, /* -1 -> 8 */
					0.584714f, 0.475874f, 0.584579f, 0.719780f, 0.567374f, 0.481001f, 0.567031f, 0.723945f, 0.324391f, 0.481304f, 0.324048f, 0.724248f, 0.270296f, 0.990269f, 0.027265f, 0.989658f, 0.269906f, 0.747017f, 0.026875f, 0.746406f, /* 9 -> 18 */
					0.556622f, 0.731031f, 0.557500f, 0.973906f, 0.313982f, 0.973672f, 0.313104f, 0.730797f, /* 20 -> 23 */
				});
			};
			public int size() { return 48; }

		};
		return new TextureData();
	};

};
