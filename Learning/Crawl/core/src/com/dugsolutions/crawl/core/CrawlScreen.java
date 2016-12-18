package com.dugsolutions.crawl.core;

import com.artemis.World;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.dugsolutions.crawl.systems.HudRenderSystem;
import com.dugsolutions.crawl.systems.SpriteAnimationSystem;
import com.dugsolutions.crawl.systems.SpriteRenderSystem;

/**
 * Created by dug on 9/22/16.
 */

public class CrawlScreen implements Screen
{
	final Game				mGame;
	final World				mWorld;
	Camera					mCamera;
	HudRenderSystem			mHudRenderSystem;
	SpriteRenderSystem		mSpriteRenderSystem;
	SpriteAnimationSystem	mSpriteAnimationSystem;

	public CrawlScreen(Game game)
	{
		mGame = game;
		mWorld = new World();

		OrthographicCamera cam = new OrthographicCamera();
		cam.setToOrtho(false, Constants.FRAME.getWidth(), Constants.FRAME.getHeight());
		mCamera = cam;
		mWorld.setSystem(mHudRenderSystem = new HudRenderSystem(mCamera), true);
		mWorld.setSystem(mSpriteRenderSystem = new SpriteRenderSystem(mCamera), true);
		mWorld.setSystem(mSpriteAnimationSystem = new SpriteAnimationSystem(), true);

		mWorld.initialize();
	}

	@Override
	public void show()
	{

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
		mHudRenderSystem.process();
	}

	@Override
	public void resize(int width, int height)
	{

	}

	@Override
	public void pause()
	{

	}

	@Override
	public void resume()
	{

	}

	@Override
	public void hide()
	{

	}

	@Override
	public void dispose()
	{

	}
}
