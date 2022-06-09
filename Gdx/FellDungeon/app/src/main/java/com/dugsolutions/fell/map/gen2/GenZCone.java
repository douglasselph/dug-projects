package com.dugsolutions.fell.map.gen2;

import com.badlogic.gdx.math.Vector3;

/**
 * A Cone Generator.
 * 
 * There is a defined boundary which indicates either a circular area or an
 * elliptical area. If it is a circle, the center of the circle is the height.
 * The edge of the circle is zero. If it is an ellipse, the two foci of the
 * ellipse and the line connecting them, is the height. The distance from the
 * foci determines what percentage of the height to use until the edge of the
 * ellipse is reached which is zero.
 */
public class GenZCone extends GenZConstant {
	protected float fA;
	protected float fB;
	protected float fAB;
	protected float fCenterX; // Center of circle and ellipse
	protected float fCenterY;
	protected boolean mIsCircle; // Otherwise ellipse which is more complicated
	
	public GenZCone(int count, float height) {
		super(count, height);
	}
	
	public GenZCone(float height) {
		super(height);
	}

	@Override
	protected void init(int startx, int starty, int endx, int endy) {
		int sizex = endx - startx;
		int sizey = endy - starty;
		fCenterX = (startx + endx) / 2;
		fCenterY = (starty + endy) / 2;
		mIsCircle = (sizex == sizey);
		fA = sizex / 2f;
		fB = sizey / 2f;
		fAB = fA * fB;
	}

	@Override
	protected void setZ(int x, int y) {

		float height;
		float percent;
		float dX = x - fCenterX;
		float dY = y - fCenterY;
		float dXSquared = dX * dX;
		float dYSquared = dY * dY;
		float dist = (float) Math.sqrt((double) (dXSquared + dYSquared));
		float maxDist;

		if (mIsCircle) {
			maxDist = fA;
		} else {
			if (dX != 0) {
				float angleT = (float) Math.atan(dY / dX);

				if (dX < 0) {
					angleT += Math.PI;
				}
				maxDist = (float) getDistOnEllipse(angleT);
			} else {
				maxDist = fB;
			}
		}
		if (dist <= maxDist) {
			percent = 1 - dist / maxDist;
			height = percent * mHeight;
			icalc.addHeight(x, y, height);

			if (icalc.genNormal() && height > 0) {
				Vector3 normal;
				normal = new Vector3(dX, dY, height);
				normal.nor();
				icalc.addNormal(x, y, normal);
			}
		}
	}

	/**
	 * Return the distance from the center the point on the ellipse would be at
	 * the given angle.
	 * 
	 * @param angleT
	 * @return
	 */
	protected double getDistOnEllipse(double angleT) {
		double termB = fB * Math.cos(angleT);
		double termA = fA * Math.sin(angleT);
		return fAB / Math.sqrt(termB * termB + termA * termA);
	}

}
