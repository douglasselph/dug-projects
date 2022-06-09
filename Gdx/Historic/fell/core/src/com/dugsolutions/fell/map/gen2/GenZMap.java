package com.dugsolutions.fell.map.gen;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.dugsolutions.fell.map.MapGrid;

/**
 * Given a MapGrid, apply a host of z generators across it.
 * 
 * @author dug
 * 
 */
public class GenZMap {

	static final boolean LOG = true;
	static final String TAG = "GenZMap";

	class MyCalcResult implements IGenZ {
		@Override
		public void addHeight(int x, int y, float z) {
			if (LOG) {
				Gdx.app.log(TAG, "addHeight(" + x + ", " + y + ", " + z + ")");
			}
			map.addElevation(x, y, z);
		}

		@Override
		public void addNormal(int x, int y, Vector3 n) {
		}

		@Override
		public boolean genNormal() {
			return false;
		}
	}

	MapGrid map;
	ArrayList<GenZBase> generators = new ArrayList<GenZBase>();
	Random random = new Random();
	int sizeLo;
	int sizeHi;
	MyCalcResult calc = new MyCalcResult();

	public GenZMap(MapGrid m) {
		map = m;
		sizeHi = (map.getMaxIndexX() < map.getMaxIndexY() ? map.getMaxIndexX()
				: map.getMaxIndexY());
		if (sizeHi > 2) {
			sizeLo = 2;
		} else {
			sizeLo = 1;
		}
	}

	public void addGenerator(GenZBase gen) {
		generators.add(gen);
	}

	public void setRandomSeed(long s) {
		random = new Random(s);
	}

	/**
	 * Set the range the bounding box for each gen can be. The X and Y sizes
	 * will randomly be between the indicated range.
	 * 
	 * @param lo
	 *            : lo size in indices
	 * @param hi
	 *            : hi size in indices
	 */
	public void setGenSize(int lo, int hi) {
		sizeLo = lo;
		sizeHi = hi;
	}

	/**
	 * Go through each generator the indicated number of times at random
	 * locations and sizes on the map.
	 * 
	 * @param count
	 */
	public void run() {
		GenZBase gen;
		// Subdivision indices
		final int maxXs = map.getMaxIndexX();
		final int maxYs = map.getMaxIndexY();
		final int sizeLen = sizeHi - sizeLo + 1;
		int xsStart; // subdivision index x
		int ysStart; // subdivision index y
		int xsSize; // subdivision size x
		int ysSize; // subdivision size y;

		while ((gen = getNextGen()) != null) {
			xsSize = random.nextInt(sizeLen) + sizeLo;
			ysSize = random.nextInt(sizeLen) + sizeLo;
			xsStart = random.nextInt(maxXs + 1 - xsSize);
			ysStart = random.nextInt(maxYs + 1 - ysSize);

			gen.gen(calc, xsStart, ysStart, xsStart + xsSize - 1, ysStart
					+ ysSize - 1);

			if (gen.getCount() <= 0) {
				generators.remove(gen);
			}
		}
		map.setElevations();
	}

	GenZBase getNextGen() {
		GenZBase gen;
		if (generators.size() == 0) {
			return null;
		}
		int idx = random.nextInt(generators.size());
		for (int i = 0; i < generators.size(); i++) {
			gen = generators.get((idx + i) % generators.size());
			if (gen.getCount() > 0) {
				return gen;
			}
		}
		return null;
	}
}
