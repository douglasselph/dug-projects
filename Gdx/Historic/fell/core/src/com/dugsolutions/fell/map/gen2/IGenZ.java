package com.dugsolutions.fell.map.gen;

import com.badlogic.gdx.math.Vector3;

public interface IGenZ {
	void addHeight(int x, int y, float z);

	void addNormal(int x, int y, Vector3 n);

	boolean genNormal();
}
