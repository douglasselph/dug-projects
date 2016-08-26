package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Sprite;

/**
 * Created by dug on 8/25/16.
 */
public class SpriteRenderSystem extends EntitySystem
{
	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Sprite>		sm;

	private OrthographicCamera	mCamera;
	private SpriteBatch			mBatch;

	@SuppressWarnings("unchecked")
	public SpriteRenderSystem(OrthographicCamera camera)
	{
		super(Aspect.getAspectForAll(Position.class, Sprite.class));
		this.mCamera = camera;
	}

	@Override
	protected void initialize()
	{
		mBatch = new SpriteBatch();
	}

	@Override
	protected boolean checkProcessing()
	{
		return true;
	}

	@Override
	protected void processEntities(ImmutableBag<Entity> entities)
	{
		for (int i = 0; i < entities.size(); i++)
		{
			process(entities.get(i));
		}
	}

	@Override
	protected void begin()
	{
		mBatch.setProjectionMatrix(mCamera.combined);
		mBatch.begin();
	}

	protected void process(Entity e)
	{
		if (pm.has(e))
		{
			Position position = pm.getSafe(e);
			Sprite sprite = sm.get(e);

			mBatch.setColor(sprite.r, sprite.g, sprite.b, sprite.a);
			float posx = position.x;
			float posy = position.y;
			mBatch.draw(sprite.sprite, posx, posy);
		}
	}


	@Override
	protected void end() {
		mBatch.end();
	}
}
