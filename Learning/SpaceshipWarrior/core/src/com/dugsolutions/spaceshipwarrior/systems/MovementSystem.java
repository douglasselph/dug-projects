package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.EntityProcessingSystem;
import com.dugsolutions.spaceshipwarrior.SpaceshipWarrior;
import com.dugsolutions.spaceshipwarrior.components.Expires;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Velocity;

public class MovementSystem extends EntityProcessingSystem
{
	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Velocity>	vm;

	public MovementSystem()
	{
		super(Aspect.getAspectForAll(Position.class, Velocity.class));
	}

	@Override
	protected void process(Entity e)
	{
		Position position = pm.get(e);
		Velocity velocity = vm.get(e);

		position.x += velocity.vx * world.delta;
		position.y += velocity.vy * world.delta;

//        Expires expires = e.getComponent(Expires.class);
//
//        if (position.x < 0)
//        {
//            position.x = 0;
//        }
//        if (position.y < 0)
//        {
//            position.y = 0;
//        }
//        if (position.x > SpaceshipWarrior.FRAME_WIDTH + 200)
//        {
//            position.x = SpaceshipWarrior.FRAME_WIDTH;
//        }
	}

}
