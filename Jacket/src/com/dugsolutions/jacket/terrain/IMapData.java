package com.dugsolutions.jacket.terrain;

import com.dugsolutions.jacket.math.Bounds2D;

public interface IMapData
{
	int getNumRows();

	int getNumCols();

	int[] getBoundary(Bounds2D bounds);

	PointInfo getPointInfo(int row, int col);

	void putZ(PointInfo info);
}
