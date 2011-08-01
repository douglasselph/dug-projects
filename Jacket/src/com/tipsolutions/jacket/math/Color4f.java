package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;



public class Color4f {
	   /**
     * the color black (0, 0, 0, 1).
     */
    public static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
    /**
     * the color black with a zero alpha value (0, 0, 0, 0).
     */
    public static final Color4f BLACK_NO_ALPHA = new Color4f(0f, 0f, 0f, 0f);
    /**
     * the color white (1, 1, 1, 1).
     */
    public static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);
    /**
     * the color gray (.2f, .2f, .2f, 1).
     */
    public static final Color4f DARK_GRAY = new Color4f(0.2f, 0.2f, 0.2f, 1.0f);
    /**
     * the color gray (.5f, .5f, .5f, 1).
     */
    public static final Color4f GRAY = new Color4f(0.5f, 0.5f, 0.5f, 1.0f);
    /**
     * the color gray (.8f, .8f, .8f, 1).
     */
    public static final Color4f LIGHT_GRAY = new Color4f(0.8f, 0.8f, 0.8f, 1.0f);
    /**
     * the color red (1, 0, 0, 1).
     */
    public static final Color4f RED = new Color4f(1f, 0f, 0f, 1f);
    /**
     * the color green (0, 1, 0, 1).
     */
    public static final Color4f GREEN = new Color4f(0f, 1f, 0f, 1f);
    /**
     * the color blue (0, 0, 1, 1).
     */
    public static final Color4f BLUE = new Color4f(0f, 0f, 1f, 1f);
    /**
     * the color yellow (1, 1, 0, 1).
     */
    public static final Color4f YELLOW = new Color4f(1f, 1f, 0f, 1f);
    /**
     * the color magenta (1, 0, 1, 1).
     */
    public static final Color4f MAGENTA = new Color4f(1f, 0f, 1f, 1f);
    /**
     * the color cyan (0, 1, 1, 1).
     */
    public static final Color4f CYAN = new Color4f(0f, 1f, 1f, 1f);
    /**
     * the color orange (251/255f, 130/255f, 0, 1).
     */
    public static final Color4f ORANGE = new Color4f(251f / 255f, 130f / 255f, 0f, 1f);
    /**
     * the color brown (65/255f, 40/255f, 25/255f, 1).
     */
    public static final Color4f BROWN = new Color4f(65f / 255f, 40f / 255f, 25f / 255f, 1f);
    /**
     * the color pink (1, 0.68f, 0.68f, 1).
     */
    public static final Color4f PINK = new Color4f(1f, 0.68f, 0.68f, 1f);

    protected float mR = 0;
    protected float mG = 0;
    protected float mB = 0;
    protected float mA = 0;
    
    public Color4f(float r, float g, float b) {
    	mR = r; mG = g; mB = b; mA = 1f;
    }
    
    public Color4f(float r, float g, float b, float a) {
    	mR = r; mG = g; mB = b; mA = a;
    }

    public float getRed() {
        return mR;
    }

    public float getGreen() {
        return mG;
    }

    public float getBlue() {
        return mB;
    }

    public float getAlpha() {
        return mA;
    }
    
    public void setRed(float r) {
        mR = r;
    }

    public void setGreen(float g) {
        mG = g;
    }

    public void setBlue(float b) {
        mB = b;
    }

    public void setAlpha(float a) {
        mA = a;
    }
    
    public void set(float r, float g, float b, float a) {
        mR = r;
        mG = g;
        mB = b;
        mA = a;
    }
    
    public Color4f put(FloatBuffer buf) {
    	buf.put(mR).put(mG).put(mB).put(mA);
    	return this;
    }
}
