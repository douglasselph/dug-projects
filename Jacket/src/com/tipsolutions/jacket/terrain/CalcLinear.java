package com.tipsolutions.jacket.terrain;

import android.util.FloatMath;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Vector3f;

/**
 * A simple cone generator. There is a defined center point where the max height is.
 * And then there is a radius where the outer edge is zero. A circle defines the entire linear
 * slope that is seen.
 */
public class CalcLinear extends CalcConstant {

	float mA; // Semi major axis or distance from center of ellipse to right edge
	float mB; // Semi minor axis or distance from center of ellipse to top edge.
	float mAB; // mA * mB
	float mCenterX;
	float mCenterY;
	boolean mIsCircle; // otherwise ellipse which is more complicated
	float mMaxDist; // used for circle only.
	
	public CalcLinear(float height) {
		super(height);
	}
	
	public CalcLinear(float height, Bounds2D bounds) {
		super(height, bounds);
	}
	
	@Override
	public Info getInfo(float x, float y) {
		if (!within(x, y)) {
			return null;
		}
		float deltaX = x - mCenterX;
		float deltaY = y - mCenterY;
		float deltaXSquared = deltaX*deltaX;
		float deltaYSquared = deltaY*deltaY;
		float dist = FloatMath.sqrt(deltaXSquared + deltaYSquared);
		float maxDist;
		
		if (mIsCircle) {
			maxDist = mMaxDist;
		} else {
			// Ellipse

			// Find point of intersection along ellipse of line crossing mid point
			// and intersecting both the point and the edge of the ellipse.
			// Below:
			//   Px,Py = point of intersection on ellipse
			//   Cx,Cy = center of ellipse
			//   x0,y0 = point defining line with center of ellipse at 0,0
			//   ... this becomes deltaX,deltaY.

			// Treat the ellipse as though the origin 0,0 is it's center.
			// To do this we need to translocate the incoming point.
			// This means we just use the delta.
			float val = mAB / FloatMath.sqrt(mA * mA * deltaYSquared + mB * mB * deltaXSquared);
			float Px = deltaX * val;
			float Py = deltaY * val;
			maxDist = FloatMath.sqrt(Px * Px + Py * Py);
		}
		if (dist < maxDist) {
    		float percent = dist / maxDist;
    		float height = mHeight * percent;
    		
    		Vector3f normal = new Vector3f(mCenterX - x, mCenterY - y, height);
    		normal.normalize();
    		
    		return new Info(height, normal);
		}
		return null;
	}

	@Override
	public void setBounds(Bounds2D bounds) {
		super.setBounds(bounds);
		mCenterX = mBounds.getMidX();
		mCenterY = mBounds.getMidY();
		mMaxDist = Math.abs(mBounds.getMidX() - mCenterX);
		mIsCircle = mBounds.isSquare();
		mA = mBounds.getSizeX()/2;
		mB = mBounds.getSizeY()/2;
		mAB = mA * mB;
	}
	
}
