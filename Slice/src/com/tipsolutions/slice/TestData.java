package com.tipsolutions.slice;

import java.util.ArrayList;

import android.util.Log;

import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;

public class TestData {

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
			tests.add(new Test("Test01", 0, 0, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test02", 45, 0, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test03", 45, 0, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test04", 45, 0, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test05", 0, 45, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test06", 0, 45, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test07", 0, 45, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test08", 0, 0, 45, new Vector3f(1,0,0)));
			tests.add(new Test("Test09", 0, 0, 45, new Vector3f(0,1,0)));
			tests.add(new Test("Test10", 0, 0, 45, new Vector3f(0,0,1)));
			tests.add(new Test("Test11", -45, 0, 0, new Vector3f(1,1,0)));
			tests.add(new Test("Test12", 0, -45, 0, new Vector3f(1,1,0)));
			tests.add(new Test("Test13", 0, 0, -45, new Vector3f(1,1,0)));
			tests.add(new Test("Test14", 315, 0, 0, new Vector3f(1,1,0)));
			tests.add(new Test("Test15", 0, 315, 0, new Vector3f(1,1,0)));
			tests.add(new Test("Test16", 0, 0, 315, new Vector3f(1,1,0)));
			tests.add(new Test("Test17", 45, 45, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test18", 45, 45, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test19", 45, 45, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test20", -45, 45, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test21", -45, 45, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test22", -45, 45, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test13", 315, 45, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test24", 315, 45, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test25", 315, 45, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test26", 45, 0, -45, new Vector3f(1,0,0)));
			tests.add(new Test("Test27", 45, 0, -45, new Vector3f(0,1,0)));
			tests.add(new Test("Test28", 45, 0, -45, new Vector3f(0,0,1)));
			tests.add(new Test("Test29", 45, 0, -45, new Vector3f(0,1,1)));
			tests.add(new Test("Test30", 45, 0, -45, new Vector3f(1,0,1)));
			tests.add(new Test("Test31", 45, 0, -45, new Vector3f(1,1,0)));
			tests.add(new Test("Test32", 45, 0, -45, new Vector3f(1,1,1)));
			tests.add(new Test("Test33", 45, 0, 315, new Vector3f(1,0,0)));
			tests.add(new Test("Test34", 45, 0, 315, new Vector3f(0,1,0)));
			tests.add(new Test("Test35", 45, 0, 315, new Vector3f(0,0,1)));
			tests.add(new Test("Test36", 190, 0, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test37", 190, 0, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test38", 190, 0, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test39", 190, 0, 0, new Vector3f(1,1,0)));
			tests.add(new Test("Test40", 0, 190, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test41", 0, 190, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test42", 0, 190, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test43", 0, 190, 0, new Vector3f(0,1,1)));
			tests.add(new Test("Test44", 0, 0, 190, new Vector3f(1,0,0)));
			tests.add(new Test("Test45", 0, 0, 190, new Vector3f(0,1,0)));
			tests.add(new Test("Test46", 0, 0, 190, new Vector3f(0,0,1)));
			tests.add(new Test("Test47", 0, 0, 190, new Vector3f(0,1,1)));
			tests.add(new Test("Test48", 300, 0, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test49", 300, 0, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test50", 300, 0, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test51", 300, 0, 0, new Vector3f(0,1,1)));
			tests.add(new Test("Test52", 0, 300, 0, new Vector3f(1,0,0)));
			tests.add(new Test("Test53", 0, 300, 0, new Vector3f(0,1,0)));
			tests.add(new Test("Test54", 0, 300, 0, new Vector3f(0,0,1)));
			tests.add(new Test("Test55", 0, 300, 0, new Vector3f(0,1,1)));
			tests.add(new Test("Test56", 0, 0, 300, new Vector3f(1,0,0)));
			tests.add(new Test("Test57", 0, 0, 300, new Vector3f(0,1,0)));
			tests.add(new Test("Test58", 0, 0, 300, new Vector3f(0,0,1)));
			tests.add(new Test("Test59", 0, 0, 300, new Vector3f(0,1,1)));
			tests.add(new Test("Test60", 261, 38, 0, new Vector3f(1,1,1)));
			float diff = 0;
			for (Test test : tests) {
				diff += test.run();
			}
			Log.d(MyApplication.TAG, "Total diff=" + diff);
		}
}
