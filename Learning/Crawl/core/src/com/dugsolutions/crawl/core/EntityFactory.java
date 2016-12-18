package com.dugsolutions.crawl.core;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.dugsolutions.crawl.components.Bounds;
import com.dugsolutions.crawl.components.Player;
import com.dugsolutions.crawl.components.Position;
import com.dugsolutions.crawl.components.Sprite;


public class EntityFactory
{
	static int	PLAYER_ID;

	public static Entity createPlayer(World world, float x, float y)
	{
		Entity e = world.createEntity();

		e.addComponent(new Position(x, y));

		Sprite sprite = new Sprite(Constants.IMG_CLAUDIUS);
		sprite.r = 93 / 255f;
		sprite.g = 255 / 255f;
		sprite.b = 129 / 255f;
		e.addComponent(sprite);
		e.addComponent(new Player());
		e.addComponent(new Bounds(43));

		PLAYER_ID = e.getId();

		world.getManager(GroupManager.class).add(e, Constants.Groups.PLAYER_PERSON);

		return e;
	}

	public static boolean IsPlayer(Entity e)
	{
		return PLAYER_ID == e.getId();
	}

}
