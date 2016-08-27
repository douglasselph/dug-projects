package com.dugsolutions.spaceshipwarrior.systems;

import com.dugsolutions.spaceshipwarrior.components.Bounds;
import com.dugsolutions.spaceshipwarrior.components.Health;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Velocity;
import com.dugsolutions.spaceshipwarrior.Constants;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.IntervalEntityProcessingSystem;

public class RemoveOffscreenShipsSystem extends IntervalEntityProcessingSystem
{
	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Bounds>		bm;

	@SuppressWarnings("unchecked")
	public RemoveOffscreenShipsSystem()
	{
		super(Aspect.getAspectForAll(Velocity.class, Position.class, Health.class, Bounds.class), 5);
	}

	@Override
	protected void process(Entity e)
	{
		Position position = pm.get(e);
		Bounds bounds = bm.get(e);

		if (position.y < 0 - bounds.radius)
		{
			e.deleteFromWorld();
		}
	}

}
