package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.EntityProcessingSystem;
import com.dugsolutions.spaceshipwarrior.components.ScaleAnimation;
import com.dugsolutions.spaceshipwarrior.components.Sprite;

public class ScaleAnimationSystem extends EntityProcessingSystem
{
	@Mapper
	ComponentMapper<ScaleAnimation>	sa;
	@Mapper
	ComponentMapper<Sprite>			sm;

	@SuppressWarnings("unchecked")
	public ScaleAnimationSystem()
	{
		super(Aspect.getAspectForAll(ScaleAnimation.class));
	}

	@Override
	protected void process(Entity e)
	{
		ScaleAnimation scaleAnimation = sa.get(e);
		if (scaleAnimation.active)
		{
			Sprite sprite = sm.get(e);

			float value = scaleAnimation.speed * world.delta;
			sprite.scaleX += value;
			sprite.scaleY += value;

			if (sprite.scaleX > scaleAnimation.max)
			{
				sprite.scaleX = scaleAnimation.max;
				scaleAnimation.active = false;
			}
			else if (sprite.scaleX < scaleAnimation.min)
			{
				sprite.scaleX = scaleAnimation.min;
				scaleAnimation.active = false;
			}
			if (sprite.scaleY > scaleAnimation.max)
			{
				sprite.scaleY = scaleAnimation.max;
				scaleAnimation.active = false;
			}
			else if (sprite.scaleY < scaleAnimation.min)
			{
				sprite.scaleY = scaleAnimation.min;
				scaleAnimation.active = false;
			}
		}
	}

}
