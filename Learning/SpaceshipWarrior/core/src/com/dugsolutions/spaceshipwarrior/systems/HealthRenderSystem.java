package com.dugsolutions.spaceshipwarrior.systems;

import com.dugsolutions.spaceshipwarrior.util.Constants;
import com.dugsolutions.spaceshipwarrior.components.Health;
import com.dugsolutions.spaceshipwarrior.components.Position;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class HealthRenderSystem extends EntityProcessingSystem
{
	@Mapper
	ComponentMapper<Position>	pm;
	@Mapper
	ComponentMapper<Health>		hm;

	private SpriteBatch			batch;
	private OrthographicCamera	camera;
	private BitmapFont			font;

	@SuppressWarnings("unchecked")
	public HealthRenderSystem(OrthographicCamera camera)
	{
		super(Aspect.getAspectForAll(Position.class, Health.class));
		this.camera = camera;
	}

	@Override
	protected void initialize()
	{
		batch = new SpriteBatch();

		Texture fontTexture = new Texture(Gdx.files.internal(Constants.FONT_IMG_NORMAL));
		fontTexture.setFilter(TextureFilter.Linear, TextureFilter.MipMapLinearLinear);
		TextureRegion fontRegion = new TextureRegion(fontTexture);
		font = new BitmapFont(Gdx.files.internal(Constants.FONT_NORMAL), fontRegion, false);
		font.setUseIntegerPositions(false);
	}

	@Override
	protected void begin()
	{
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
	}

	@Override
	protected void process(Entity e)
	{
		Position position = pm.get(e);
		Health health = hm.get(e);

		int percentage = MathUtils.round(health.health / health.maxHealth * 100f);

		font.draw(batch, percentage + "%", position.x, position.y);
	}

	@Override
	protected void end()
	{
		batch.end();
	}
}
