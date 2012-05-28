package com.tipsolutions.bugplug.map;

import com.tipsolutions.bugplug.R;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.model.Model;
import com.tipsolutions.jacket.terrain.CalcConstant;
import com.tipsolutions.jacket.terrain.CalcGroup;
import com.tipsolutions.jacket.terrain.CalcLinear;
import com.tipsolutions.jacket.terrain.CalcParabola;
import com.tipsolutions.jacket.terrain.CalcStore;
import com.tipsolutions.jacket.terrain.TerrainGrid;
import com.tipsolutions.jacket.terrain.TerrainGrids;

public class Map {

	TerrainGrids mTerrainGrids;
	TextureManager mTM;
	
	public Map(TextureManager tm) {
		mTM = tm;
		mTerrainGrids = new TerrainGrids();
		
		TerrainGrid grid = new TerrainGrid()
			.setBounds(new Bounds2D(0, 0, 11f, 2f))
			.setGranularity(10, 10);
		CalcGroup calcGroup = new CalcGroup();
		calcGroup.add(new CalcConstant(4f, new Bounds2D(0, 0, 11f, 2f)));
		calcGroup.add(new CalcLinear(2f, new Bounds2D(3f, 0f, 6f, 2f)));
		calcGroup.add(new CalcParabola(5f, 0.4f, new Bounds2D(8f, 0f, 11f, 2f)));
		CalcStore calcStore = new CalcStore(calcGroup);
		grid.setCompute(calcStore);
		grid.setTexture(mTM.getTexture(R.drawable.hardrock));
		grid.init();
		mTerrainGrids.addGrid(grid);

		grid = new TerrainGrid()
			.setBounds(new Bounds2D(0, 2, 2f, 20f))
			.setGranularity(10, 10);
		calcGroup = new CalcGroup();
		calcGroup.add(new CalcConstant(4f, new Bounds2D(0, 0, 11f, 2f)));
		calcStore = new CalcStore(calcGroup);
		grid.setCompute(calcStore);
		grid.setTexture(mTM.getTexture(R.drawable.water));
		grid.init();
		
		grid = new TerrainGrid()
			.setBounds(new Bounds2D(2, 2, 11f, 20f))
			.setGranularity(10, 10);
		calcGroup = new CalcGroup();
		calcGroup.add(new CalcConstant(1f, new Bounds2D(2, 2, 11f, 20f)));
		calcGroup.add(new CalcLinear(1f, new Bounds2D(5f, 5f, 10f, 15f)));
		calcGroup.add(new CalcParabola(1f, 0.4f, new Bounds2D(3f, 16f, 8f, 20f)));
		calcStore = new CalcStore(calcGroup);
		grid.setCompute(calcStore);
		grid.setTexture(mTM.getTexture(R.drawable.dirt));
		grid.init();
		
		mTerrainGrids.addGrid(grid);
	}
	
	public void onDraw(MatrixTrackingGL gl) {
		mTerrainGrids.onDraw(gl);
	}
	
	public Model getPrimaryModel() {
		return mTerrainGrids.getGrid(0);
	}

}
