package com.dugsolutions.spaceshipwarrior.systems;

import com.dugsolutions.spaceshipwarrior.components.ParallaxStar;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.Constants;

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

		if (position.y < 0)
		{
			position.y = Constants.FRAME_HEIGHT;
		}
		if (position.x < 0)
		{
			position.x += Constants.FRAME_WIDTH;
		}
		if (position.x > Constants.FRAME_WIDTH)
		{
			position.x -= Constants.FRAME_WIDTH;
		}
	}

}
