package com.tipsolutions.jacket.math;

public class MathUtils {

	 /** A "close to zero" double epsilon value for use */
    public static final double EPSILON = 2.220446049250313E-16d;
    
    public static double inverseSqrt(final double dValue) {
        return 1 / Math.sqrt(dValue);
    }
    
    public static double clamp(double v) {
    	while (v < 0) {
    		v += Constants.TWO_PI;
    	}
    	while (v >= Constants.TWO_PI) {
    		v -= Constants.TWO_PI;
    	}
    	return v;
    }
    
    public static float clamp(float v) {
    	while (v < 0) {
    		v += Constants.TWO_PI;
    	}
    	while (v >= Constants.TWO_PI) {
    		v -= Constants.TWO_PI;
    	}
    	return v;
    }
    
}
