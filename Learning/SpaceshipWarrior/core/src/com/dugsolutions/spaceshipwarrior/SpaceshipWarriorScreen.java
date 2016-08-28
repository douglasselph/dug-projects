package com.dugsolutions.spaceshipwarrior;

import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dugsolutions.spaceshipwarrior.systems.CollisionSystem;
import com.dugsolutions.spaceshipwarrior.systems.EntitySpawningTimerSystem;
import com.dugsolutions.spaceshipwarrior.systems.ExpiringSystem;
import com.dugsolutions.spaceshipwarrior.systems.HealthRenderSystem;
import com.dugsolutions.spaceshipwarrior.systems.HudRenderSystem;
import com.dugsolutions.spaceshipwarrior.systems.MovementSystem;
import com.dugsolutions.spaceshipwarrior.systems.ParallaxStarRepeatingSystem;
import com.dugsolutions.spaceshipwarrior.systems.PlayerInputSystem;
import com.dugsolutions.spaceshipwarrior.systems.RemoveOffscreenShipsSystem;
import com.dugsolutions.spaceshipwarrior.systems.ScaleAnimationSystem;
import com.dugsolutions.spaceshipwarrior.systems.SpriteRenderSystem;
import com.dugsolutions.spaceshipwarrior.util.Constants;
import com.dugsolutions.spaceshipwarrior.util.EntityFactory;

/**
 * Created by dug on 8/25/16.
 */
public class SpaceshipWarriorScreen implements Screen
{
	static final float		ASPECT_RATIO	= (float) Constants.FRAME.getWidth() / (float) Constants.FRAME.getHeight();
	static final boolean	PERSPECTIVE		= false;

	Camera					mCamera;
	Game					mGame;
	World					mWorld;
	SpriteRenderSystem		mSpriteRenderSystem;
	HudRenderSystem			mHudRenderSystem;
	HealthRenderSystem		mHealthRenderSystem;

	public SpaceshipWarriorScreen(Game game)
	{
		if (PERSPECTIVE)
		{
			PerspectiveCamera cam = new PerspectiveCamera(67, Constants.FRAME.getWidth(), Constants.FRAME.getHeight());
			cam.position.set(Constants.FRAME.getMidX(), Constants.FRAME.getMidY(), 10f);
			cam.lookAt(Constants.FRAME.getMidX(), Constants.FRAME.getMidY(), 0);
			cam.near = 1f;
			cam.far = 100f;
			cam.update();
			mCamera = cam;
		}
		else
		{
			OrthographicCamera cam = new OrthographicCamera();
			cam.setToOrtho(false, Constants.FRAME.getWidth(), Constants.FRAME.getHeight());
			mCamera = cam;
		}

		mGame = game;

		mWorld = new World();

		mWorld.setSystem(new PlayerInputSystem(mCamera));
		mWorld.setSystem(new MovementSystem());
		mWorld.setSystem(new EntitySpawningTimerSystem());
		mWorld.setSystem(new CollisionSystem());
		mWorld.setSystem(new ScaleAnimationSystem());
		mWorld.setSystem(new ParallaxStarRepeatingSystem());
		mWorld.setSystem(new RemoveOffscreenShipsSystem());
		mWorld.setSystem(new ExpiringSystem());
		// mWorld.setSystem(new SoundEffectSystem());

		mWorld.setSystem(mSpriteRenderSystem = new SpriteRenderSystem(mCamera), true);
		mWorld.setSystem(mHudRenderSystem = new HudRenderSystem(mCamera), true);
		mWorld.setSystem(mHealthRenderSystem = new HealthRenderSystem(mCamera), true);

		mWorld.setManager(new GroupManager());

		mWorld.initialize();

		EntityFactory.createPlayer(mWorld, Constants.FRAME.getMidX(), Constants.PLAYER_Y).addToWorld();

		for (int i = 0; i < com.dugsolutions.spaceshipwarrior.util.Constants.NUM_STARS; i++)
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
		// mHealthRenderSystem.process();
		// mHudRenderSystem.process();
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
			scale = (float) height / (float) Constants.FRAME.getHeight();
			crop.x = (width - Constants.FRAME.getWidth() * scale) / 2f;
		}
		else if (aspectRatio < ASPECT_RATIO)
		{
			scale = (float) width / (float) Constants.FRAME.getWidth();
			crop.y = (height - Constants.FRAME.getHeight() * scale) / 2f;
		}
		else
		{
			scale = (float) width / (float) Constants.FRAME.getWidth();
		}

		float w = (float) Constants.FRAME.getWidth() * scale;
		float h = (float) Constants.FRAME.getHeight() * scale;
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

}
