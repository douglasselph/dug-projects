package com.dugsolutions.spaceshipwarrior.systems;

import com.dugsolutions.spaceshipwarrior.components.ParallaxStar;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.util.Constants;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.IntervalEntityProcessingSystem;

public class ParallaxStarRepeatingSystem extends IntervalEntityProcessingSystem
{
	@Mapper
	ComponentMapper<Position>	pm;

	@SuppressWarnings("unchecked")
	public ParallaxStarRepeatingSystem()
	{
		super(Aspect.getAspectForAll(ParallaxStar.class, Position.class), 1);
	}

	@Override
	protected void process(Entity e)
	{
		Position position = pm.get(e);

		if (position.y < Constants.FRAME.getMinY())
		{
			position.y = Constants.FRAME.getMaxY();
		}
		if (position.x < Constants.FRAME.getMinX())
		{
			position.x += Constants.FRAME.getWidth();
		}
		if (position.x > Constants.FRAME.getMaxX())
		{
			position.x -= Constants.FRAME.getWidth();
		}
	}

}
