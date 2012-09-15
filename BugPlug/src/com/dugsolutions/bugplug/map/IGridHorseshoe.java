package com.dugsolutions.bugplug.map;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.terrain.ModelGrid;

public interface IGridHorseshoe
{
	public void onDraw(GL10 gl);

	public ModelGrid getGround();

	public ModelGrid getWater();

}
