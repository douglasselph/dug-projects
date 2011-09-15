package com.tipsolutions.slice;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.Matrix;
import android.util.Log;

import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class TestData {

	public void run() {
		testMatrixAddRotate();
		testMatrixInvert();
		testUnproject();
		
		float [] projection = new float[16];
		Matrix.frustumM(projection, 0, -1, 1, -1, 1, 1, 1000);
		Log.d("PROJ", showM(projection));
		float [] invert = new float[16];
		Matrix.invertM(invert, 0, projection, 0);
		Log.d("INVERT", showM(invert));
	}
	
	String showM(float [] m) {
		StringBuffer sbuf = new StringBuffer();
		for (int r = 0; r < 4; r++) {
			sbuf.append(r);
			sbuf.append("=[");
			for (int c = 0; c < 4; c++) {
				if (c > 0) {
					sbuf.append(",");
				}
				sbuf.append(m[c*4+r]);
			}
			sbuf.append("]\n");
		}
		return sbuf.toString();
	}
	
	String showV(float [] v) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[");
		boolean first = true; 
		for (float f : v) {
			if (first) {
				first = false;
			} else {
				sbuf.append(",");
			}
			sbuf.append(f);
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
	final float ZERO_THRESHOLD = 0.0001f;

	float assertEquals(String what, float v1, float v2) {
		float diff = Math.abs(v1-v2);
		if (Math.abs(diff) > ZERO_THRESHOLD) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("ERROR: ");
			sbuf.append(what);
			sbuf.append("->");
			sbuf.append(v1);
			sbuf.append("!=");
			sbuf.append(v2);
			sbuf.append(", diff=");
			sbuf.append(diff);
			Log.e(MyApplication.TAG, sbuf.toString());
		}
		return diff;
	}

	float assertEquals(String what, Vector3f v1, Vector3f v2) {
		float diff = 0;
		if (!v1.equals(v2)){
			double dx = Math.abs(v1.getX()-v2.getX());
			double dy = Math.abs(v1.getY()-v2.getY());
			double dz = Math.abs(v1.getZ()-v2.getZ());
			diff += dx + dy + dz;
			if (dx > ZERO_THRESHOLD ||
					dy > ZERO_THRESHOLD ||
					dz > ZERO_THRESHOLD) {
				StringBuffer sbuf = new StringBuffer();
				sbuf.append("ERROR: ");
				sbuf.append(what);
				sbuf.append("->");
				sbuf.append(v1.toString());
				sbuf.append("!=");
				sbuf.append(v2.toString());
				sbuf.append(", diff=");
				sbuf.append(diff);
				Log.e(MyApplication.TAG, sbuf.toString());
			}
		}
		return diff;
	}
	
	float assertEquals(String what, Matrix4f m1, Matrix4f m2) {
		float diff = 0;
		if (!m1.equals(m2)) {
			for (int i = 0; i < m1.getArray().length; i++) {
				diff += Math.abs(m1.getArray()[i]-m2.getArray()[i]);
			}
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("ERROR: ");
			sbuf.append(what);
			sbuf.append(" ");
			sbuf.append(m1.toString());
			sbuf.append(" != ");
			sbuf.append(m2.toString());
			sbuf.append(", diff=");
			sbuf.append(diff);
			Log.e(MyApplication.TAG, sbuf.toString());
		}
		return diff;
	}

	void testMatrixAddRotate() {

		class Test {
			String mName;
			Vector3f mVecInit;
			float mRotX, mRotY, mRotZ;

			Test(String name, int rx, int ry, int rz, Vector3f initVec) {
				mName = name;
				mRotX = (float) Math.toRadians(rx);
				mRotY = (float) Math.toRadians(ry);
				mRotZ = (float) Math.toRadians(rz);
				mVecInit = initVec;
			}

			Vector3f getExpected() {
				Matrix3f m = new Matrix3f();
				m.setRotate(mRotX, mRotY, mRotZ);
				return m.apply(mVecInit,null);
			}

			float run() {
				float diff = 0;
				Quaternion quat = new Quaternion();
				quat.fromAngles(mRotX, mRotY, mRotZ);

				Vector3f vec = new Vector3f(quat.apply(mVecInit, null));
				diff += assertEquals(mName, vec, getExpected());

				return diff;
			}
		};
		ArrayList<Test> tests = new ArrayList<Test>();
		tests.add(new Test("Vec01", 0, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec02", 45, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec03", 45, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec04", 45, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec05", 0, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec06", 0, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec07", 0, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec08", 0, 0, 45, new Vector3f(1,0,0)));
		tests.add(new Test("Vec09", 0, 0, 45, new Vector3f(0,1,0)));
		tests.add(new Test("Vec10", 0, 0, 45, new Vector3f(0,0,1)));
		tests.add(new Test("Vec11", -45, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Vec12", 0, -45, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Vec13", 0, 0, -45, new Vector3f(1,1,0)));
		tests.add(new Test("Vec14", 315, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Vec15", 0, 315, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Vec16", 0, 0, 315, new Vector3f(1,1,0)));
		tests.add(new Test("Vec17", 45, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec18", 45, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec19", 45, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec20", -45, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec21", -45, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec22", -45, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec13", 315, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec24", 315, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec25", 315, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec26", 45, 0, -45, new Vector3f(1,0,0)));
		tests.add(new Test("Vec27", 45, 0, -45, new Vector3f(0,1,0)));
		tests.add(new Test("Vec28", 45, 0, -45, new Vector3f(0,0,1)));
		tests.add(new Test("Vec29", 45, 0, -45, new Vector3f(0,1,1)));
		tests.add(new Test("Vec30", 45, 0, -45, new Vector3f(1,0,1)));
		tests.add(new Test("Vec31", 45, 0, -45, new Vector3f(1,1,0)));
		tests.add(new Test("Vec32", 45, 0, -45, new Vector3f(1,1,1)));
		tests.add(new Test("Vec33", 45, 0, 315, new Vector3f(1,0,0)));
		tests.add(new Test("Vec34", 45, 0, 315, new Vector3f(0,1,0)));
		tests.add(new Test("Vec35", 45, 0, 315, new Vector3f(0,0,1)));
		tests.add(new Test("Vec36", 190, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec37", 190, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec38", 190, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec39", 190, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Vec40", 0, 190, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec41", 0, 190, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec42", 0, 190, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec43", 0, 190, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Vec44", 0, 0, 190, new Vector3f(1,0,0)));
		tests.add(new Test("Vec45", 0, 0, 190, new Vector3f(0,1,0)));
		tests.add(new Test("Vec46", 0, 0, 190, new Vector3f(0,0,1)));
		tests.add(new Test("Vec47", 0, 0, 190, new Vector3f(0,1,1)));
		tests.add(new Test("Vec48", 300, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec49", 300, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec50", 300, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec51", 300, 0, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Vec52", 0, 300, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Vec53", 0, 300, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Vec54", 0, 300, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Vec55", 0, 300, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Vec56", 0, 0, 300, new Vector3f(1,0,0)));
		tests.add(new Test("Vec57", 0, 0, 300, new Vector3f(0,1,0)));
		tests.add(new Test("Vec58", 0, 0, 300, new Vector3f(0,0,1)));
		tests.add(new Test("Vec59", 0, 0, 300, new Vector3f(0,1,1)));
		tests.add(new Test("Vec60", 261, 38, 0, new Vector3f(1,1,1)));
		float diff = 0;
		for (Test test : tests) {
			diff += test.run();
		}
		Log.d(MyApplication.TAG, "Total diff=" + diff);
	}
	
	void testMatrixInvert() {
		
		class Test {
			String mName;
			Matrix4f mPre;
			Matrix4f mPost;

			Test(String name, Matrix4f pre, Matrix4f post) {
				mName = name;
				mPre = pre;
				mPost = post;
			}

			float run() {
				float diff = 0;
				Matrix4f m = new Matrix4f(mPre).invert();
				diff += assertEquals(mName + "_A", m, mPost);
				m.invert();
				diff += assertEquals(mName + "_B", m, mPre);
				return diff;
			}
		};
		ArrayList<Test> tests = new ArrayList<Test>();
		tests.add(new Test("Invert1", 
				new Matrix4f(1.000f,  0.100f,  0.200f,  0.300f,
							 0.400f,  2.000f,  0.500f,  0.600f,
							 0.700f,  0.800f,  3.000f,  0.900f,
							-0.100f, -0.200f, -0.300f,  4.000f),
				new Matrix4f(
						 1.056072669667f, -0.030172196710f, -0.071241180699f, -0.058650355061f,
						 -0.161025213701f,  0.536552311447f, -0.083648867055f, -0.049584960602f,
						 -0.204383438715f, -0.140696753398f,  0.365820715800f, -0.045876390142f,
						  0.003021798153f,  0.015521054150f,  0.021473080815f,  0.242613763833f
						  )));
		float diff = 0;
		for (Test test : tests) {
			diff += test.run();
		}
		Log.d(MyApplication.TAG, "Total diff=" + diff);
	}
	
	void testUnproject() {
		
		final float [] IDENTITY = { 1, 0, 0, 0, 
								    0, 1, 0, 0,
								    0, 0, 1, 0,
								    0, 0, 0, 1 };
		final float WIDTH = 480;
		final float HEIGHT = 560;
		final float LEFT = -1f;
		final float RIGHT = 1f;
		final float TOP = 1f;
		final float BOTTOM = -1f;
		final float NEAR = 1f;
		final float FAR = 1000f;
		
		class Test {
			
			float [] mPlanes = new float[6];
			float [] mModelview = IDENTITY;
			float [] mProjection = new float[16];
			float [] mLoc = new float[3];
			float [] mViewport = new float[4];
			
			Vector3f mResultWin;
			Vector3f mResultWorld;
			float mResultWorldW;
			float mResultClipW;
			
			Test(float x, float y, float z) {
				mPlanes[0] = LEFT;
				mPlanes[1] = RIGHT;
				mPlanes[2] = BOTTOM;
				mPlanes[3] = TOP;
				mPlanes[4] = NEAR;
				mPlanes[5] = FAR;
				Matrix.frustumM(mProjection, 0, mPlanes[0], mPlanes[1], mPlanes[2], mPlanes[3], mPlanes[4], mPlanes[5]);
				mLoc[0] = x;
				mLoc[1] = y;
				mLoc[2] = z;
				mViewport[0] = 0;
				mViewport[1] = 0;
				mViewport[2] = WIDTH;
				mViewport[3] = HEIGHT;
			}
			
			float vWidth() { return mViewport[2] - mViewport[0]; }
			float vHeight() { return mViewport[3] - mViewport[1]; }
			
			void forward() {
				float [] worldVec = new float[4];
				
				worldVec[0] = mLoc[0];
				worldVec[1] = mLoc[1];
				worldVec[2] = mLoc[2];
				worldVec[3] = 1;

				float [] eyeVec = new float[4];
				Matrix.multiplyMV(eyeVec, 0, mModelview, 0, worldVec, 0);

				float [] clipVec = new float[4];
				Matrix.multiplyMV(clipVec, 0, mProjection, 0, eyeVec, 0);

				float [] normDevCoords = new float[4];
				float div = 1f/clipVec[3];
				normDevCoords[0] = clipVec[0] * div;
				normDevCoords[1] = clipVec[1] * div;
				normDevCoords[2] = clipVec[2] * div;
				normDevCoords[3] = 1f;

				float [] winCoords = new float[3];
				winCoords[0] = (normDevCoords[0] *.5f + .5f) * vWidth() + mViewport[0];
				winCoords[1] = (normDevCoords[1] *.5f + .5f) * vHeight() + mViewport[1];
				winCoords[2] = (1 + normDevCoords[2]) * .5f;

				mResultWin = new Vector3f(winCoords[0], winCoords[1], winCoords[2]);
				mResultClipW = clipVec[3];
			}
			
			void testReverse(float z, float clipW) {
				Vector3f v;
				reverse(v = new Vector3f(mResultWin.getX(), mResultWin.getY(), z), clipW);
				StringBuffer sbuf = new StringBuffer();
				sbuf.append("Reverse:");
				sbuf.append(v.toString());
				sbuf.append(", C");
				sbuf.append(clipW);
				sbuf.append("->");
				sbuf.append(mResultWorld.toString());
				sbuf.append(", resultW=");
				sbuf.append(mResultWorldW);
				Log.d("DEBUG", sbuf.toString());
			}
			
			void reverse() {
				reverse(mResultWin, mResultClipW);
			}
			
			void reverse(Vector3f win, float clipW) {
				// Reverse
				float [] winCoordsR = new float[3];
				
				winCoordsR[0] = win.getX();
				winCoordsR[1] = win.getY();
				winCoordsR[2] = win.getZ();

				float [] normDevCoordsR = new float[4];
				normDevCoordsR[0] = (((winCoordsR[0] - mViewport[0]) / vWidth()) - .5f)/.5f;
				normDevCoordsR[1] = (((winCoordsR[1] - mViewport[1])/ vHeight()) - .5f)/.5f;
				normDevCoordsR[2] = ((winCoordsR[2] / .5f - 1));
				normDevCoordsR[3] = 1f;

				float [] clipVecR = new float[4];
				clipVecR[0] = normDevCoordsR[0] * clipW;
				clipVecR[1] = normDevCoordsR[1] * clipW;
				clipVecR[2] = normDevCoordsR[2] * clipW;
				clipVecR[3] = clipW;

				float [] scratch = new float[16];
				Matrix.multiplyMM(scratch, 0, mProjection, 0, mModelview, 0);
				Matrix.invertM(scratch, 0, scratch, 0);

				float [] worldVecR = new float[4];
				Matrix.multiplyMV(worldVecR, 0, scratch, 0, clipVecR, 0);

				mResultWorld = new Vector3f(worldVecR[0], worldVecR[1], worldVecR[2]);
				mResultWorldW = worldVecR[3];
			}
			
			void run() {
				forward();
				Log.d("DEBUG", "***" + showV(mPlanes));
				Log.d("DEBUG", "Vec:" + new Vector3f(mLoc).toString() + "->" + mResultWin.toString() + ", clipW=" + mResultClipW);
				reverse();
				Log.d("DEBUG", "Reverse:" + mResultWorld.toString() + ", ResultW=" + mResultWorldW);
				testReverse(0, 1);
				testReverse(0.5f, 1);
				testReverse(1, 1);
				testReverse(0, 2);
				testReverse(0, 3);
				testReverse(0, 4);
				testReverse(0, 10);
				testReverse(0, 20);
			}
			
		};
		ArrayList<Test> tests = new ArrayList<Test>();
		
		for (int z = -1; z >= -3; z--) {
    		tests.add(new Test(0, 0, z));
    		tests.add(new Test(LEFT+(RIGHT-LEFT)/4, BOTTOM+(TOP-BOTTOM)/4, z));
    		tests.add(new Test(RIGHT, TOP, z));
		}
		for (Test t : tests) {
			t.run();
		}
	}
}
