package com.dugsolutions.spaceshipwarrior.util;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.math.MathUtils;
import com.dugsolutions.spaceshipwarrior.components.Bounds;
import com.dugsolutions.spaceshipwarrior.components.ColorAnimation;
import com.dugsolutions.spaceshipwarrior.components.ScaleByDist;
import com.dugsolutions.spaceshipwarrior.components.Expires;
import com.dugsolutions.spaceshipwarrior.components.Health;
import com.dugsolutions.spaceshipwarrior.components.ParallaxStar;
import com.dugsolutions.spaceshipwarrior.components.Player;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.ScaleAnimation;
import com.dugsolutions.spaceshipwarrior.components.SoundEffect;
import com.dugsolutions.spaceshipwarrior.components.Sprite;
import com.dugsolutions.spaceshipwarrior.components.Velocity;

public class EntityFactory
{
	static int	PLAYER_ID;

	public static Entity createPlayer(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));

		Sprite sprite = new Sprite(Constants.IMG_FIGHTER, Sprite.Layer.ACTORS_3);
		sprite.r = 93 / 255f;
		sprite.g = 255 / 255f;
		sprite.b = 129 / 255f;
		e.addComponent(sprite);
		e.addComponent(new Player());
		e.addComponent(new Bounds(43));

		if (!Constants.CENTRAL_PLAYER)
		{
			e.addComponent(new Velocity());
		}
		PLAYER_ID = e.getId();

		world.getManager(GroupManager.class).add(e, Constants.Groups.PLAYER_SHIP);
		return e;
	}

	public static boolean IsPlayer(Entity e)
	{
		return PLAYER_ID == e.getId();
	}

	public static Entity createBullet(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));
		e.addComponent(new Sprite(Constants.IMG_BULLET, Sprite.Layer.PARTICLES));
		e.addComponent(new Velocity(0, 800));
		e.addComponent(new Expires(2f));
		e.addComponent(new Bounds(5));
		e.addComponent(new SoundEffect(SoundEffect.EFFECT.PEW));

		if (Constants.CENTRAL_PLAYER)
		{
			e.addComponent(new ScaleByDist());

		}
		world.getManager(GroupManager.class).add(e, Constants.Groups.PLAYER_BULLETS);

		return e;
	}

	public static Entity createEnemyShip(World world, String name, Sprite.Layer layer, float health, float x, float y,
			float vx, float vy, float boundsRadius)
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
		e.addComponent(new Health(health));
		e.addComponent(new Bounds(boundsRadius));

		if (Constants.CENTRAL_PLAYER)
		{
			e.addComponent(new ScaleByDist());
		}
		world.getManager(GroupManager.class).add(e, Constants.Groups.ENEMY_SHIPS);

		return e;
	}

	public static Entity createParticle(World world, float x, float y)
	{
		float scaleDistY;

		if (Constants.CENTRAL_PLAYER)
		{
			scaleDistY = Constants.computeScaleFromY(y);
		}
		else
		{
			scaleDistY = 1;
		}
		float scale = MathUtils.random(0.3f, 0.6f);

		Entity e = world.createEntity();

		Position position = new Position();
		position.x = x;
		position.y = y;
		e.addComponent(position);

		Sprite sprite = new Sprite();
		sprite.name = Constants.IMG_PARTICLE;
		sprite.scaleX = sprite.scaleY = scale * scaleDistY;
		sprite.r = 1;
		sprite.g = 216 / 255f;
		sprite.b = 0;
		sprite.a = 0.5f;
		sprite.layer = Sprite.Layer.PARTICLES;
		e.addComponent(sprite);

		float radians = MathUtils.random(2 * MathUtils.PI);
		float magnitude = MathUtils.random(400f);

		Velocity velocity = new Velocity(magnitude * MathUtils.cos(radians), magnitude * MathUtils.sin(radians));
		e.addComponent(velocity);

		Expires expires = new Expires();
		expires.delay = 1;
		e.addComponent(expires);

		ColorAnimation colorAnimation = new ColorAnimation();
		colorAnimation.alphaAnimate = true;
		colorAnimation.alphaSpeed = -1f;
		colorAnimation.alphaMin = 0f;
		colorAnimation.alphaMax = 1f;
		colorAnimation.repeat = false;
		e.addComponent(colorAnimation);

		return e;
	}

	public static void createSmallExplosion(World world, float x, float y)
	{
		Entity e = createExplosion(world, x, y, 0.1f);

		e.addComponent(new SoundEffect(SoundEffect.EFFECT.SMALLASPLODE));
		e.addToWorld();

		createBurst(world, x, y);
	}

	public static void createBigExplosion(World world, float x, float y)
	{
		Entity e = createExplosion(world, x, y, 0.5f);

		e.addComponent(new SoundEffect(SoundEffect.EFFECT.ASPLODE));
		e.addToWorld();
	}

	static void createBurst(World world, float x, float y)
	{
		for (int i = 0; i < 50; i++)
		{
			EntityFactory.createParticle(world, x, y).addToWorld();
		}
	}

	public static Entity createExplosion(World world, float x, float y, float scale)
	{
		float scaleDistY;

		if (Constants.CENTRAL_PLAYER)
		{
			scaleDistY = Constants.computeScaleFromY(y);
		}
		else
		{
			scaleDistY = 1;
		}
		float useScale = scale * scaleDistY;

		Entity e = world.createEntity();

		Position position = new Position();
		position.x = x;
		position.y = y;
		e.addComponent(position);

		Sprite sprite = new Sprite();
		sprite.name = Constants.IMG_EXPLOSION;
		sprite.scaleX = sprite.scaleY = useScale;
		sprite.r = 1;
		sprite.g = 216 / 255f;
		sprite.b = 0;
		sprite.a = 0.5f;
		sprite.layer = Sprite.Layer.PARTICLES;
		e.addComponent(sprite);

		Expires expires = new Expires();
		expires.delay = 0.5f;
		e.addComponent(expires);

		ScaleAnimation scaleAnimation = new ScaleAnimation();
		scaleAnimation.active = true;
		scaleAnimation.max = useScale;
		scaleAnimation.min = useScale / 100f;
		scaleAnimation.speed = -3.0f;
		scaleAnimation.repeat = false;
		e.addComponent(scaleAnimation);

		return e;
	}

	public static Entity createStar(World world)
	{
		Entity e = world.createEntity();

		Position position = new Position();
		position.x = MathUtils.random(Constants.FRAME.getMinX(), Constants.FRAME.getMaxX());
		position.y = MathUtils.random(Constants.FRAME.getMinY(), Constants.FRAME.getMaxY());
		e.addComponent(position);

		Sprite sprite = new Sprite();
		sprite.name = Constants.IMG_STAR;
		sprite.scaleX = sprite.scaleY = MathUtils.random(0.5f, 1f);
		sprite.a = MathUtils.random(0.1f, 0.5f);
		sprite.layer = Sprite.Layer.BACKGROUND;
		e.addComponent(sprite);

		Velocity velocity = new Velocity();
		velocity.vy = MathUtils.random(-10f, -60f);
		e.addComponent(velocity);
		e.addComponent(new ParallaxStar());

		ColorAnimation colorAnimation = new ColorAnimation();
		colorAnimation.alphaAnimate = true;
		colorAnimation.repeat = true;
		colorAnimation.alphaSpeed = MathUtils.random(0.2f, 0.7f);
		colorAnimation.alphaMin = 0.1f;
		colorAnimation.alphaMax = 0.5f;
		e.addComponent(colorAnimation);

		return e;
	}

}
