package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.dugsolutions.spaceshipwarrior.EntityFactory;
import com.dugsolutions.spaceshipwarrior.Adjust;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Velocity;

public class MovementSystem extends EntityProcessingSystem
{
	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Velocity>	vm;

	Vector3						tmp	= new Vector3();

	public MovementSystem()
	{
		super(Aspect.getAspectForAll(Position.class, Velocity.class));
	}

	@Override
	protected void process(Entity e)
	{
		Position position = pm.get(e);
		Velocity velocity = vm.get(e);

		position.x += velocity.vx * world.getDelta();
		position.y += velocity.vy * world.getDelta();

		if (!EntityFactory.IsPlayer(e))
		{
            tmp.set(position.x, position.y, 0);

            Matrix4 mx = Adjust.getInstance().getMx();
            tmp.mul(mx);

            position.x = tmp.x;
            position.y = tmp.y;
		}
	}

}
