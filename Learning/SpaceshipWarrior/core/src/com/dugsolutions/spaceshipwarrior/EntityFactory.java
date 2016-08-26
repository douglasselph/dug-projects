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

		Sprite sprite = new Sprite(Constants.FIGHTER, Sprite.Layer.ACTORS_3);
		sprite.r = 93 / 255f;
		sprite.g = 255 / 255f;
		sprite.b = 129 / 255f;
		e.addComponent(sprite);
		e.addComponent(new Velocity());
		e.addComponent(new Player());

		return e;
	}

	public static Entity createBullet(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));
		e.addComponent(new Sprite(Constants.BULLET, Sprite.Layer.PARTICLES));
		e.addComponent(new Velocity(0, 800));
		e.addComponent(new Expires(2f));

		return e;
	}

	public static Entity createEnemyShip(World world, String name, Sprite.Layer layer, float x, float y, float vx,
			float vy)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));

		Sprite sprite = new Sprite();
		sprite.name = name;
		sprite.r = 255 / 255f;
		sprite.g = 0 / 255f;
		sprite.b = 142 / 255f;
		sprite.layer = layer;
		e.addComponent(sprite);

		e.addComponent(new Velocity(vx, vy));

		return e;
	}

}
