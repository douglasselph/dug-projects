package com.tipsolutions.jacket.math;

public class ComputeBounds {

	public float minX = 0;
	public float minY = 0;
	public float minZ = 0;
	public float maxX = 0;
	public float maxY = 0;
	public float maxZ = 0;
	protected boolean initialized = false;

	public ComputeBounds() {}

	public void apply(float x, float y, float z) {
		if (!initialized) {
			minX = x;
			minY = y;
			minZ = z;
			maxX = x;
			maxY = y;
			maxZ = z;
			initialized = true;
		} else {
			if (x < minX) {
				minX = x;
			} else if (x > maxX) {
				maxX = x;
			}
			if (y < minY) {
				minY = y;
			} else if (y > maxY) {
				maxY = y;
			}
			if (z < minZ) {
				minZ = z;
			} else if (z > maxZ) {
				maxZ = z;
			}
		}
	}
	
	public Bounds3D getBounds() {
		return new Bounds3D(this);
	}
}
