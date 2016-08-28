package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.systems.VoidEntitySystem;
import com.artemis.utils.Timer;
import com.badlogic.gdx.math.MathUtils;
import com.dugsolutions.spaceshipwarrior.util.Constants;
import com.dugsolutions.spaceshipwarrior.util.EntityFactory;
import com.dugsolutions.spaceshipwarrior.components.Sprite;

public class EntitySpawningTimerSystem extends VoidEntitySystem
{
	private Timer	timer1;
	private Timer	timer2;
	private Timer	timer3;

	public EntitySpawningTimerSystem()
	{
		timer1 = new Timer(2, true)
		{
			@Override
			public void execute()
			{
				EntityFactory.createEnemyShip(world, "enemy1", Sprite.Layer.ACTORS_3, 10,
						MathUtils.random(Constants.FRAME.getMinX() + 10, Constants.FRAME.getMaxX() - 10),
						Constants.FRAME.getMaxY() + 50, 0, -40, 20).addToWorld();
			}
		};
		timer2 = new Timer(6, true)
		{
			@Override
			public void execute()
			{
				EntityFactory.createEnemyShip(world, "enemy2", Sprite.Layer.ACTORS_2, 20,
						MathUtils.random(Constants.FRAME.getMinX() + 10, Constants.FRAME.getMaxX() - 10),
						Constants.FRAME.getMaxY() + 50, 0, -30, 40).addToWorld();
			}
		};
		timer3 = new Timer(12, true)
		{
			@Override
			public void execute()
			{
				EntityFactory.createEnemyShip(world, "enemy3", Sprite.Layer.ACTORS_1, 60,
						MathUtils.random(Constants.FRAME.getMinX() + 10, Constants.FRAME.getMaxX() - 10),
						Constants.FRAME.getMaxY() + 50, 0, -20, 70).addToWorld();
			}
		};
	}

	@Override
	protected void processSystem()
	{
		timer1.update(world.delta);
		timer2.update(world.delta);
		timer3.update(world.delta);
	}

}
