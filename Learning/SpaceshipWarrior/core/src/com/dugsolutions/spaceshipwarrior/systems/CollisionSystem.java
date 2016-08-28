package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.managers.GroupManager;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;
import com.artemis.utils.Utils;
import com.dugsolutions.spaceshipwarrior.Constants;
import com.dugsolutions.spaceshipwarrior.EntityFactory;
import com.dugsolutions.spaceshipwarrior.components.Bounds;
import com.dugsolutions.spaceshipwarrior.components.Health;
import com.dugsolutions.spaceshipwarrior.components.Position;

public class CollisionSystem extends EntitySystem
{
	class CollisionPair
	{
		private ImmutableBag<Entity>	groupEntitiesA;
		private ImmutableBag<Entity>	groupEntitiesB;
		private CollisionHandler		handler;

		public CollisionPair(String group1, String group2, CollisionHandler handler)
		{
			groupEntitiesA = world.getManager(GroupManager.class).getEntities(group1);
			groupEntitiesB = world.getManager(GroupManager.class).getEntities(group2);
			this.handler = handler;
		}

		public void checkForCollisions()
		{
			for (int a = 0; groupEntitiesA.size() > a; a++)
			{
				for (int b = 0; groupEntitiesB.size() > b; b++)
				{
					Entity entityA = groupEntitiesA.get(a);
					Entity entityB = groupEntitiesB.get(b);
					if (collisionExists(entityA, entityB))
					{
						handler.handleCollision(entityA, entityB);
					}
				}
			}
		}

		boolean collisionExists(Entity e1, Entity e2)
		{
			Position p1 = pm.get(e1);
			Position p2 = pm.get(e2);

			Bounds b1 = bm.get(e1);
			Bounds b2 = bm.get(e2);

			return Utils.doCirclesCollide(p1.x, p1.y, b1.radius, p2.x, p2.y, b2.radius);
		}
	}

	interface CollisionHandler
	{
		void handleCollision(Entity a, Entity b);
	}

	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Bounds>		bm;
	@Mapper
	ComponentMapper<Health>		hm;

	Bag<CollisionPair>			collisionPairs;

	@SuppressWarnings("unchecked")
	public CollisionSystem()
	{
		super(Aspect.getAspectForAll(Position.class, Bounds.class));
	}

	@Override
	public void initialize()
	{
		collisionPairs = new Bag<CollisionPair>();

		collisionPairs.add(new CollisionPair(Constants.Groups.PLAYER_BULLETS, Constants.Groups.ENEMY_SHIPS,
				new CollisionHandler()
				{
					@Override
					public void handleCollision(Entity bullet, Entity ship)
					{
						Health health = hm.get(ship);
						health.health -= 10;

                        Position bp = pm.get(bullet);
                        EntityFactory.createSmallExplosion(world, bp.x, bp.y);

						if (health.health <= 0)
						{
                            bp = pm.get(ship);
                            EntityFactory.createBigExplosion(world, bp.x, bp.y);

							ship.deleteFromWorld();
						}
                        bullet.deleteFromWorld();
                    }
				}));
	}

	@Override
	protected void processEntities(ImmutableBag<Entity> entities)
	{
		for (int i = 0; collisionPairs.size() > i; i++)
		{
			collisionPairs.get(i).checkForCollisions();
		}
	}

	@Override
	protected boolean checkProcessing()
	{
		return true;
	}
}
