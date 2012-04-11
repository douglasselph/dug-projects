package com.tipsolutions.jacket.terrain;

import com.tipsolutions.jacket.math.Vector3f;

public interface ComputeValue {
	float getHeight(float x, float y);
	Vector3f getNormal(float x, float y);
}
