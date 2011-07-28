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

};


class pigeon_Feather001 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 0.000000f,
		                    0.000000f, -0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, -1.000000f, -0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather002 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 7.000000f,
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

};


class pigeon_Feather003 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, 2.803703f,
		                    0.000000f, 1.000000f, 0.000000f, -1.074204f,
		                    0.000000f, 0.000000f, 1.000000f, -0.496519f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather004 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 21.790791f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.005079f,
		                    0.018135f, -0.969417f, 0.244750f, 0.903443f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather005 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 22.642740f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.041811f,
		                    0.018135f, -0.969417f, 0.244750f, 1.503029f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather006 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 23.482168f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.086014f,
		                    0.018135f, -0.969417f, 0.244750f, 2.010453f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather007 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 24.242670f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.112970f,
		                    0.018135f, -0.969417f, 0.244750f, 2.606348f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather008 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 24.286856f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.063358f,
		                    0.018135f, -0.969417f, 0.244750f, 3.173347f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather009 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 25.208656f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.086996f,
		                    0.018135f, -0.969417f, 0.244750f, 3.989614f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather010 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 26.322222f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.110516f,
		                    0.018135f, -0.969417f, 0.244750f, 5.027482f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};


class pigeon_Feather011 extends ShapeData {

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(0.842601f, 0.146594f, 0.518202f, 27.130249f,
		                    -0.538232f, 0.196829f, 0.819490f, -10.148383f,
		                    0.018135f, -0.969417f, 0.244750f, 5.564192f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}
	@Override protected float _getMinX() { return -1.000000f; }
	@Override protected float _getMaxX() { return 1.000001f; }
	@Override protected float _getMinY() { return -3.485010f; }
	@Override protected float _getMaxY() { return 3.485012f; }
	@Override protected float _getMinZ() { return -6.573984f; }
	@Override protected float _getMaxZ() { return -6.573984f; }

	@Override
	protected FloatData getVertexData() {
		class VertexData implements FloatData {
			public void fill(FloatBuffer buf) {
				buf.put(1.000001f).put(3.485010f).put(-6.573984f); /* 0 */
				buf.put(1.000000f).put(-3.485010f).put(-6.573984f); /* 1 */
				buf.put(-1.000000f).put(-3.485010f).put(-6.573984f); /* 2 */
				buf.put(-0.999999f).put(3.485012f).put(-6.573984f); /* 3 */
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

};
