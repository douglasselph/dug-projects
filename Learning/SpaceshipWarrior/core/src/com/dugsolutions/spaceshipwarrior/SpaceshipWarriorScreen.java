package com.dugsolutions.spaceshipwarrior;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dugsolutions.spaceshipwarrior.components.Player;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Sprite;
import com.dugsolutions.spaceshipwarrior.components.Velocity;
import com.dugsolutions.spaceshipwarrior.systems.CollisionSystem;
import com.dugsolutions.spaceshipwarrior.systems.EntitySpawningTimerSystem;
import com.dugsolutions.spaceshipwarrior.systems.HealthRenderSystem;
import com.dugsolutions.spaceshipwarrior.systems.HudRenderSystem;
import com.dugsolutions.spaceshipwarrior.systems.MovementSystem;
import com.dugsolutions.spaceshipwarrior.systems.ParallaxStarRepeatingSystem;
import com.dugsolutions.spaceshipwarrior.systems.PlayerInputSystem;
import com.dugsolutions.spaceshipwarrior.systems.RemoveOffscreenShipsSystem;
import com.dugsolutions.spaceshipwarrior.systems.ScaleAnimationSystem;
import com.dugsolutions.spaceshipwarrior.systems.SoundEffectSystem;
import com.dugsolutions.spaceshipwarrior.systems.SpriteRenderSystem;

/**
 * Created by dug on 8/25/16.
 */
public class SpaceshipWarriorScreen implements Screen
{
	static final float	ASPECT_RATIO	= (float) Constants.FRAME_WIDTH / (float) Constants.FRAME_HEIGHT;

	OrthographicCamera	mCamera;
	Game				mGame;
	World				mWorld;
	SpriteRenderSystem	mSpriteRenderSystem;
	HudRenderSystem		mHudRenderSystem;
	HealthRenderSystem	mHealthRenderSystem;
	PlayerInputSystem	mPlayerInputSystem;

	public SpaceshipWarriorScreen(Game game)
	{
		mCamera = new OrthographicCamera();
		mCamera.setToOrtho(false, 1280, 900);

		mGame = game;

		mWorld = new World();

		mWorld.setSystem(mPlayerInputSystem = new PlayerInputSystem(mCamera));
		mWorld.setSystem(new MovementSystem());
		mWorld.setSystem(new EntitySpawningTimerSystem());
		mWorld.setSystem(new CollisionSystem());
		mWorld.setSystem(new ScaleAnimationSystem());
		mWorld.setSystem(new ParallaxStarRepeatingSystem());
		mWorld.setSystem(new RemoveOffscreenShipsSystem());
		mWorld.setSystem(new SoundEffectSystem());

		mWorld.setSystem(mSpriteRenderSystem = new SpriteRenderSystem(mCamera), true);
		mWorld.setSystem(mHudRenderSystem = new HudRenderSystem(mCamera), true);
		mWorld.setSystem(mHealthRenderSystem = new HealthRenderSystem(mCamera), true);

		mWorld.setManager(new GroupManager());

		mWorld.initialize();

		EntityFactory.createPlayer(mWorld, Constants.FRAME_WIDTH / 2, 150).addToWorld();

		for (int i = 0; i < Constants.NUM_STARS; i++)
		{
			EntityFactory.createStar(mWorld).addToWorld();
		}
        Adjust.Init();
	}

	@Override
	public void render(float delta)
	{
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		mCamera.update();

        Adjust.getInstance().next(delta);

		mWorld.setDelta(delta);
		mWorld.process();
		mSpriteRenderSystem.process();
		mHealthRenderSystem.process();
		mHudRenderSystem.process();
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public void hide()
	{

	}

	@Override
	public void pause()
	{

	}

	@Override
	public void resize(int width, int height)
	{
		// Need to copy more code in to get this to work.
		float aspectRatio = (float) width / (float) height;
		float scale = 1f;
		Vector2 crop = new Vector2(0f, 0f);

		if (aspectRatio > ASPECT_RATIO)
		{
			scale = (float) height / (float) Constants.FRAME_HEIGHT;
			crop.x = (width - Constants.FRAME_WIDTH * scale) / 2f;
		}
		else if (aspectRatio < ASPECT_RATIO)
		{
			scale = (float) width / (float) Constants.FRAME_WIDTH;
			crop.y = (height - Constants.FRAME_HEIGHT * scale) / 2f;
		}
		else
		{
			scale = (float) width / (float) Constants.FRAME_WIDTH;
		}

		float w = (float) Constants.FRAME_WIDTH * scale;
		float h = (float) Constants.FRAME_HEIGHT * scale;
		Rectangle viewport = new Rectangle(crop.x, crop.y, w, h);
		// mPlayerInputSystem.setViewport(viewport);
	}

	@Override
	public void resume()
	{

	}

	@Override
	public void show()
	{
	}

	void soundOn()
	{
	}

	void soundOff()
	{
	}

}
