/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.ShapeData;
import com.tipsolutions.jacket.math.Matrix4f;


class pigeon_Feather extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, -5.000000f,
		                    0.000000f, 1.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-0.999999f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather001 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 6.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -7.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather002 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 7.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -6.500000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather003 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 8.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -6.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather004 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 9.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -5.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather005 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 11.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -3.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather006 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 10.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -4.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather007 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 12.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -2.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather008 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 13.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, -1.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather009 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 14.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather010 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 15.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, 1.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};


class pigeon_Feather011 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 16.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, 2.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000000f; }
	@Override protected float _getMinY() { return -3.485011f; }
	@Override protected float _getMaxY() { return 3.485011f; }
	@Override protected float _getMinZ() { return 0.000000f; }
	@Override protected float _getMaxZ() { return 0.000000f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(3.485010f).put(0.000000f); /* 0 */
				buf.put(1.000000f).put(-3.485011f).put(0.000000f); /* 1 */
				buf.put(-1.000000f).put(-3.485011f).put(0.000000f); /* 2 */
				buf.put(-1.000000f).put(3.485011f).put(0.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new VertexData();
	};

	@Override
	protected FloatData getNormalData() {
		class NormalData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 0 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 2 */
				buf.put(0.000000f).put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 12; }

		};
		return new NormalData();
	};

	@Override
	protected ShortData getIndexData() {
		class IndexData implements ShortData {
			public void fill(ShortBuffer buf) {
				buf.put((short)0).put((short)3).put((short)2); /* 0 */
				buf.put((short)0).put((short)2).put((short)1); /* 1 */
			};
			public int size() { return 6; }

		};
		return new IndexData();
	};

	@Override
	protected String _getTextureFilename() { return "feather_real.png"; }
	@Override
	protected FloatData getTextureData() {
		class TextureData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000000f).put(1.000000f); /* 0 */
				buf.put(1.000000f).put(0.000000f); /* 1 */
				buf.put(0.000000f).put(0.000000f); /* 2 */
				buf.put(0.000000f).put(1.000000f); /* 3 */
			};
			public int size() { return 8; }

		};
		return new TextureData();
	};

};
