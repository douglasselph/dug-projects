package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.DelayedEntityProcessingSystem;
import com.artemis.systems.EntityProcessingSystem;
import com.dugsolutions.spaceshipwarrior.components.Expires;

public class ExpiringSystem extends EntityProcessingSystem
{
	@Mapper
	ComponentMapper<Expires>	em;

	public ExpiringSystem()
	{
		super(Aspect.getAspectForAll(Expires.class));
	}

	@Override
	protected boolean checkProcessing()
	{
		return true;
	}

	@Override
	protected void process(Entity e)
	{
		Expires exp = em.get(e);
		exp.delay -= world.getDelta();
		if (exp.delay <= 0)
		{
			e.deleteFromWorld();
		}
	}
}
