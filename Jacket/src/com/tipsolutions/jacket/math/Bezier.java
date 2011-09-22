package com.tipsolutions.jacket.math;

public class Bezier {
	
	Vector3f mCalc = new Vector3f();
	Vector3f mCalc2 = new Vector3f();

	public class Curve {
		Vector3f [] mP = new Vector3f[3];
		
		public Curve(Vector3f p1, Vector3f p2, Vector3f p3) {
			mP[0] = p1;
			mP[1] = p2;
			mP[2] = p3;
		}
		
		public Curve(Vector3f [] p) {
			mP = p;
		}
		
		public Vector3f evaluate(float t) {
			float invT = 1-t;
			mCalc.set(mP[0]);
			mCalc.multiply(invT*invT);
			mCalc2.set(mP[1]);
			mCalc2.multiply(2*invT);
			mCalc.add(mCalc2);
			mCalc2.set(mP[2]);
			mCalc2.multiply(t*t);
			mCalc.add(mCalc2);
			return mCalc;
		}
	};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

}
