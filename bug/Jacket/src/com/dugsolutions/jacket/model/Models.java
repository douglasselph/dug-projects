package com.dugsolutions.jacket.model;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import com.dugsolutions.jacket.math.ComputeBounds;

public class Models extends Model
{

	protected ArrayList<Models>	mChildren;

	public Models getChild(int i)
	{
		return mChildren.get(i);
	}

	public ArrayList<Models> getChildren()
	{
		return mChildren;
	}

	@Override
	protected void computeBounds(ComputeBounds computeBounds)
	{
		super.computeBounds(computeBounds);

		if (getChildren() != null)
		{
			for (Models child : getChildren())
			{
				child.computeBounds(computeBounds);
				computeBounds.apply(child.getBounds().getMinX(), child.getBounds().getMinY(), child.getBounds()
						.getMinZ());
				computeBounds.apply(child.getBounds().getMaxX(), child.getBounds().getMaxY(), child.getBounds()
						.getMaxZ());
			}
		}
	}

	@Override
	protected void onDrawPost(GL10 gl)
	{
		if (mChildren != null)
		{
			for (Models model : mChildren)
			{
				model.onDraw(gl);
			}
		}
	}

}
