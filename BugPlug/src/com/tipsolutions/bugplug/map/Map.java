package com.tipsolutions.bugplug.map;

import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.terrain.CalcConstant;
import com.tipsolutions.jacket.terrain.CalcGroup;
import com.tipsolutions.jacket.terrain.CalcLinear;
import com.tipsolutions.jacket.terrain.CalcParabola;
import com.tipsolutions.jacket.terrain.CalcStore;
import com.tipsolutions.jacket.terrain.TerrainGrid;

public class Map {

	TerrainGrid mTerrainGrid;
	
	public Map() {
		mTerrainGrid = new TerrainGrid()
			.setDimension(11f, 20f)
			.setGranularity(10, 10);
		CalcGroup calcGroup = new CalcGroup();
		calcGroup.add(new CalcConstant(2f, new Bounds2D(0, 0, 11f, 3f)));
		calcGroup.add(new CalcLinear(3f, new Bounds2D(0, 3f, 6f, 6f)));
		calcGroup.add(new CalcParabola(5f, 0.4f, new Bounds2D(6f, 3f, 11f, 6f)));
		CalcStore calcStore = new CalcStore(calcGroup);
		mTerrainGrid.setCompute(calcStore);
		mTerrainGrid.init();
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		mTerrainGrid.onDraw(gl);
	}

}
