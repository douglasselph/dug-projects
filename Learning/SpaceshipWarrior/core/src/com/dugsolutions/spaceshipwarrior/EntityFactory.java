package com.dugsolutions.spaceshipwarrior;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.math.MathUtils;
import com.dugsolutions.spaceshipwarrior.components.Bounds;
import com.dugsolutions.spaceshipwarrior.components.ColorAnimation;
import com.dugsolutions.spaceshipwarrior.components.Expires;
import com.dugsolutions.spaceshipwarrior.components.Health;
import com.dugsolutions.spaceshipwarrior.components.ParallaxStar;
import com.dugsolutions.spaceshipwarrior.components.Player;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.ScaleAnimation;
import com.dugsolutions.spaceshipwarrior.components.Sprite;
import com.dugsolutions.spaceshipwarrior.components.Velocity;

public class EntityFactory
{

	public static Entity createPlayer(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));
		e.addComponent(new Sprite("fighter.png"));
		e.addComponent(new Velocity());
		e.addComponent(new Player());

		return e;
	}

	public static Entity createBullet(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));
		e.addComponent(new Sprite("bullet.png"));
		e.addComponent(new Velocity(0, 800));
		e.addComponent(new Expires(1f));

		return e;
	}

}
