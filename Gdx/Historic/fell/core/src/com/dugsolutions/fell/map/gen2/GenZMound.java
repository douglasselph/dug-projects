package com.dugsolutions.fell.map.gen;

import com.badlogic.gdx.math.Vector3;

/**
 * A center point where the height is at max.
 * 
 * Then a slope defines a parabolic slope outward toward the edge, max circle
 * radius, where the value is zero.
 */
public class GenZMound extends GenZCone {
	protected float mPA;

	public GenZMound(float height) {
		super(height);
	}
	
	public GenZMound(int count, float height) {
		super(count, height);
	}

	@Override
	protected void setZ(int x, int y) {
		float dX = x - fCenterX;
		float dY = y - fCenterY;
		float dXSquared = dX * dX;
		float dYSquared = dY * dY;
		float height;
		float dist = (float) Math.sqrt(dXSquared + dYSquared);

		if (mIsCircle) {
			height = -mPA * dist * dist + mHeight;
		} else {
			float maxDist;

			if (dX != 0) {
				float angleT = (float) Math.atan(dY / dX);

				if (dX < 0) {
					angleT += Math.PI;
				}
				maxDist = (float) getDistOnEllipse(angleT);
			} else {
				maxDist = fB;
			}
			// Parabola along the line in the ellipsoid of interest.
			height = -(mHeight / (maxDist * maxDist)) * dist * dist + mHeight;
		}
		if (height > 0) {
			icalc.addHeight(x, y, height);

			if (icalc.genNormal()) {
				Vector3 normal;
				normal = new Vector3(dX, dY, -height);
				normal.nor();
				icalc.addNormal(x, y, normal);
			}
		}
	}

	@Override
	protected void init(int startx, int starty, int endx, int endy) {
		super.init(startx, starty, endx, endy);
		mPA = mHeight / (fA * fA);
	}

}
