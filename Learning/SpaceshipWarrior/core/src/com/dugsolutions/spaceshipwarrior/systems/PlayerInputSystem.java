package com.dugsolutions.spaceshipwarrior.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.dugsolutions.spaceshipwarrior.Adjust;
import com.dugsolutions.spaceshipwarrior.EntityFactory;
import com.dugsolutions.spaceshipwarrior.components.Player;
import com.dugsolutions.spaceshipwarrior.components.Position;
import com.dugsolutions.spaceshipwarrior.components.Velocity;

public class PlayerInputSystem extends EntityProcessingSystem implements InputProcessor
{
	@Mapper
	ComponentMapper<Velocity>	vm;
	@Mapper
	ComponentMapper<Position>	pm;

	OrthographicCamera			camera;
	Vector3						mouseVector;

	int							ax, ay;
	final int					thruster	= 40;
	final float					drag		= 0.4f;
	boolean						shoot;

	public PlayerInputSystem(OrthographicCamera camera)
	{
		super(Aspect.getAspectForAll(Position.class, Player.class));
		this.camera = camera;
		this.mouseVector = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
	}

	@Override
	protected void initialize()
	{
		Gdx.input.setInputProcessor(this);
	}

	@Override
	protected void process(Entity e)
	{
		mouseVector.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(mouseVector);

		if (ax != 0)
		{
			Adjust.getInstance().inc(ax, 0);
			ax = 0;
		}

		// float incx = (ax - drag * vel.vx) * world.getDelta();
		// float incy = (ay - drag * vel.vy) * world.getDelta();

		if (shoot)
		{
			Position pos = pm.get(e);
			EntityFactory.createBullet(world, pos.x + 7, pos.y + 40).addToWorld();
			EntityFactory.createBullet(world, pos.x + 60, pos.y + 40).addToWorld();
		}
	}

	@Override
	public boolean keyDown(int keycode)
	{
		// if (keycode == Input.Keys.UP)
		// ay = thruster;
		// if (keycode == Input.Keys.DOWN)
		// ay = -thruster;
		if (keycode == Input.Keys.RIGHT)
			ax = -thruster;
		if (keycode == Input.Keys.LEFT)
			ax = thruster;
		if (keycode == Input.Keys.SPACE)
			shoot = true;
		return false;
	}

	@Override
	public boolean keyUp(int keycode)
	{
		if (keycode == Input.Keys.UP)
			ay = 0;
		if (keycode == Input.Keys.DOWN)
			ay = 0;
		if (keycode == Input.Keys.RIGHT)
			ax = 0;
		if (keycode == Input.Keys.LEFT)
			ax = 0;
		if (keycode == Input.Keys.SPACE)
			shoot = false;
		return false;
	}

	@Override
	public boolean keyTyped(char character)
	{
		return false;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button)
	{
		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button)
	{
		return true;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer)
	{
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY)
	{
		return false;
	}

	@Override
	public boolean scrolled(int amount)
	{
		return false;
	}

}
