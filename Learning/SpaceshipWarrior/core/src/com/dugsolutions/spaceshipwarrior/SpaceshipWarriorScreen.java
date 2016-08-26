package com.dugsolutions.spaceshipwarrior;

import com.artemis.Entity;
import com.artemis.World;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.dugsolutions.spaceshipwarrior.components.Player;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Sprite;
import com.dugsolutions.spaceshipwarrior.components.Velocity;
import com.dugsolutions.spaceshipwarrior.systems.MovementSystem;
import com.dugsolutions.spaceshipwarrior.systems.PlayerInputSystem;
import com.dugsolutions.spaceshipwarrior.systems.SpriteRenderSystem;

/**
 * Created by dug on 8/25/16.
 */
public class SpaceshipWarriorScreen implements Screen
{
	OrthographicCamera	mCamera;
	Game				mGame;
	World				mWorld;
	SpriteRenderSystem	mSpriteRenderSystem;

	public SpaceshipWarriorScreen(Game game)
	{
		mCamera = new OrthographicCamera();
		mCamera.setToOrtho(false, 1280, 900);

		mGame = game;

		mWorld = new World();
		mSpriteRenderSystem = mWorld.setSystem(new SpriteRenderSystem(mCamera), true);

		mWorld.setSystem(new PlayerInputSystem(mCamera));
		mWorld.setSystem(new MovementSystem());

		mWorld.initialize();

		EntityFactory.createPlayer(mWorld, 150, 150).addToWorld();
	}

	@Override
	public void render(float delta)
	{
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		mCamera.update();

		mWorld.setDelta(delta);
		mWorld.process();
		mSpriteRenderSystem.process();
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
