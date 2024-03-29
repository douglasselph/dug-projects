package com.dugsolutions.crawl.systems;

import java.util.HashMap;

import com.artemis.ComponentMapper;
import com.artemis.annotations.Mapper;
import com.artemis.systems.VoidEntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.dugsolutions.crawl.components.Position;
import com.dugsolutions.crawl.components.Sprite;
import com.dugsolutions.crawl.core.Constants;

import static com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Position;

public class HudRenderSystem extends VoidEntitySystem
{
	@Mapper
	ComponentMapper<Position> pm;
	@Mapper
	ComponentMapper<Sprite> sm;

	private HashMap<String, AtlasRegion>	regions;
	private TextureAtlas					textureAtlas;
	private SpriteBatch						batch;
	private Camera							camera;
	private BitmapFont						font;

	public HudRenderSystem(Camera camera)
	{
		this.camera = camera;
	}

	@Override
	protected void initialize()
	{
		regions = new HashMap<String, AtlasRegion>();
		textureAtlas = new TextureAtlas(Constants.CLAUDIAS_ATLAS);
		for (AtlasRegion r : textureAtlas.getRegions())
		{
			regions.put(r.name, r);
		}
		batch = new SpriteBatch();

		Texture fontTexture = new Texture(Gdx.files.internal(Constants.FONT_IMG_HUD));
		fontTexture.setFilter(TextureFilter.Linear, TextureFilter.MipMapLinearLinear);
		TextureRegion fontRegion = new TextureRegion(fontTexture);
		font = new BitmapFont(Gdx.files.internal(Constants.FONT_HUD), fontRegion, false);
		font.setUseIntegerPositions(false);
	}

	@Override
	protected void begin()
	{
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
	}

	@Override
	protected void processSystem()
	{
		batch.setColor(1, 1, 1, 1);
		font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 20, Constants.FRAME.getMaxY() - 20);
		font.draw(batch, "Active entities: " + world.getEntityManager().getActiveEntityCount(), 20,
				Constants.FRAME.getMaxY() - 40);
		font.draw(batch, "Total created: " + world.getEntityManager().getTotalCreated(), 20,
				Constants.FRAME.getMaxY() - 60);
		font.draw(batch, "Total deleted: " + world.getEntityManager().getTotalDeleted(), 20,
				Constants.FRAME.getMaxY() - 80);
	}

	@Override
	protected void end()
	{
		batch.end();
	}

}
