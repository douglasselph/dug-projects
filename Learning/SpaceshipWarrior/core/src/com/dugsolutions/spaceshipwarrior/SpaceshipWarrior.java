package com.dugsolutions.spaceshipwarrior;

import com.badlogic.gdx.Game;

public class SpaceshipWarrior extends Game
{
	@Override
	public void create()
	{
		setScreen(new SpaceshipWarriorScreen(this));
	}

	public static void main(String[] args)
	{
	}

}
