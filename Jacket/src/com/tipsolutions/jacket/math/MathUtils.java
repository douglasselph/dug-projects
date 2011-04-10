package com.tipsolutions.jacket.math;

public class MathUtils {

	 /** A "close to zero" double epsilon value for use */
    public static final double EPSILON = 2.220446049250313E-16d;
    
    public static double inverseSqrt(final double dValue) {
        return 1 / Math.sqrt(dValue);
    }
}
