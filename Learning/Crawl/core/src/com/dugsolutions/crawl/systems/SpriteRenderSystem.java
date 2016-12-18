package com.dugsolutions.crawl.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.dugsolutions.crawl.components.Position;
import com.dugsolutions.crawl.components.Sprite;
import com.dugsolutions.crawl.components.SpriteAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SpriteRenderSystem extends EntitySystem
{
	@Mapper
	ComponentMapper<Position>			pm;
	@Mapper
	ComponentMapper<Sprite>				sm;
	@Mapper
	ComponentMapper<SpriteAnimation>	sam;

	private Camera						mCamera;
	private SpriteBatch					batch;
	private TextureAtlas				atlas;
	private List<Entity>				sortedEntities;

	@SuppressWarnings("unchecked")
	public SpriteRenderSystem(Camera camera)
	{
		super(Aspect.getAspectForAll(Position.class, Sprite.class));
		this.mCamera = camera;
	}

	@Override
	protected void initialize()
	{
		batch = new SpriteBatch();

		atlas = new TextureAtlas(Gdx.files.internal("textures/pack.atlas"), Gdx.files.internal("textures"));
		sortedEntities = new ArrayList<Entity>();
	}

	@Override
	protected boolean checkProcessing()
	{
		return true;
	}

	@Override
	protected void processEntities(ImmutableBag<Entity> entities)
	{
		for (Entity e : sortedEntities)
		{
			process(e);
		}
	}

	@Override
	protected void begin()
	{
		batch.setProjectionMatrix(mCamera.combined);
		batch.begin();
	}

	protected void process(Entity e)
	{
		if (pm.has(e))
		{
			Position position = pm.getSafe(e);
			Sprite sprite = sm.get(e);

			TextureRegion spriteRegion = sprite.region;
			batch.setColor(sprite.r, sprite.g, sprite.b, sprite.a);

			int width = spriteRegion.getRegionWidth();
			int height = spriteRegion.getRegionHeight();

			sprite.region.setRegion(sprite.x, sprite.y, width, height);

			float posX = position.x - (spriteRegion.getRegionWidth() / 2 * sprite.scaleX);
			float posY = position.y - (spriteRegion.getRegionHeight() / 2 * sprite.scaleX);
			batch.draw(spriteRegion, posX, posY, 0, 0, spriteRegion.getRegionWidth(), spriteRegion.getRegionHeight(),
					sprite.scaleX, sprite.scaleY, sprite.rotation);
		}
	}

	@Override
	protected void end()
	{
		batch.end();
	}

	@Override
	protected void inserted(Entity e)
	{
		Sprite sprite = sm.get(e);
		sortedEntities.add(e);
		TextureRegion reg = atlas.findRegion(sprite.name);
		sprite.region = reg;
		sprite.x = reg.getRegionX();
		sprite.y = reg.getRegionY();
		sprite.width = reg.getRegionWidth();
		sprite.height = reg.getRegionHeight();
		if (sam.has(e))
		{
			SpriteAnimation anim = sam.getSafe(e);
			anim.animation = new Animation(anim.frameDuration, atlas.findRegions(sprite.name), anim.playMode);
		}

		Collections.sort(sortedEntities, new Comparator<Entity>()
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
		sortedEntities.remove(e);
	}
}