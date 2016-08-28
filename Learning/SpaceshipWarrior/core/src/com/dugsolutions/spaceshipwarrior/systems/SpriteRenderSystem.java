package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.dugsolutions.spaceshipwarrior.util.Constants;
import com.dugsolutions.spaceshipwarrior.components.ScaleByDist;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Sprite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dug on 8/25/16.
 */
public class SpriteRenderSystem extends EntitySystem
{
	@Mapper
	ComponentMapper<Position>					pm;
	@Mapper
	ComponentMapper<Sprite>						sm;
	@Mapper
	ComponentMapper<ScaleByDist>				em;

	OrthographicCamera							mCamera;
	SpriteBatch									mBatch;
	TextureAtlas								mAtlas;
	HashMap<String, TextureAtlas.AtlasRegion>	mRegions;
	Bag<TextureAtlas.AtlasRegion>				mRegionsByEntity;
	List<Entity>								mSortedEntities;

	@SuppressWarnings("unchecked")
	public SpriteRenderSystem(OrthographicCamera camera)
	{
		super(Aspect.getAspectForOne(Position.class, Sprite.class, ScaleByDist.class));
		this.mCamera = camera;
	}

	@Override
	protected void initialize()
	{
		mBatch = new SpriteBatch();
		mAtlas = new TextureAtlas(Gdx.files.internal(Constants.PACK_ATLAS), Gdx.files.internal(Constants.TEXTURE));
		mRegions = new HashMap<>();
		for (TextureAtlas.AtlasRegion r : mAtlas.getRegions())
		{
			mRegions.put(r.name, r);
		}
		mRegionsByEntity = new Bag<>();
		mSortedEntities = new ArrayList<>();
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
		if (pm.has(e) && sm.has(e))
		{
			Position position = pm.get(e);
			Sprite sprite = sm.get(e);

			TextureAtlas.AtlasRegion spriteRegion = mRegionsByEntity.get(e.getId());
			mBatch.setColor(sprite.r, sprite.g, sprite.b, sprite.a);
			float scaleX = sprite.scaleX;
			float scaleY = sprite.scaleY;

			if (em.has(e))
			{
				float scale = Constants.computeScaleFromY(position.y);
				scaleX *= scale;
				scaleY *= scale;
			}
			float posX = position.x - (spriteRegion.getRegionWidth() / 2 * scaleX);
			float posY = position.y - (spriteRegion.getRegionHeight() / 2 * scaleY);
			mBatch.draw(spriteRegion, posX, posY, 0, 0, spriteRegion.getRegionWidth(), spriteRegion.getRegionHeight(),
					scaleX, scaleY, sprite.rotation);
		}
	}

	@Override
	protected void end()
	{
		mBatch.end();
	}

	@Override
	protected void inserted(Entity e)
	{
		Sprite sprite = sm.get(e);
		mRegionsByEntity.set(e.getId(), mRegions.get(sprite.name));

		mSortedEntities.add(e);

		Collections.sort(mSortedEntities, new Comparator<Entity>()
		{
			@Override
			public int compare(Entity e1, Entity e2)
			{
				Sprite s1 = sm.get(e1);
				Sprite s2 = sm.get(e2);
				return s1.layer.compareTo(s2.layer);
			}
		});
	}

	@Override
	protected void removed(Entity e)
	{
		mRegionsByEntity.set(e.getId(), null);
		mSortedEntities.remove(e);
	}
}
