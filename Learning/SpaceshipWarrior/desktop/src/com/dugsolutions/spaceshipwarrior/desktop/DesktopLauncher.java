package com.dugsolutions.spaceshipwarrior.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.dugsolutions.spaceshipwarrior.Constants;
import com.dugsolutions.spaceshipwarrior.SpaceshipWarrior;

public class DesktopLauncher
{
	public static void main(String[] arg)
	{
		LwjglApplicationConfiguration lwjglApplicationConfiguration = new LwjglApplicationConfiguration();
		lwjglApplicationConfiguration.fullscreen = false;
		lwjglApplicationConfiguration.width = Constants.FRAME_WIDTH;
		lwjglApplicationConfiguration.height = Constants.FRAME_HEIGHT;
		lwjglApplicationConfiguration.vSyncEnabled = false;
		lwjglApplicationConfiguration.title = "Spaceship Warrior";
		new LwjglApplication(new SpaceshipWarrior(), lwjglApplicationConfiguration);
	}
}
