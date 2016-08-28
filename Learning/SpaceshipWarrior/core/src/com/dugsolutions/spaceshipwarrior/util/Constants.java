package com.dugsolutions.spaceshipwarrior.util;

import java.awt.Rectangle;

public class Constants
{
	public static final SimpleBounds FRAME = new SimpleBounds(0, 0, 1280, 900);

	public class Groups
	{
		public static final String	PLAYER_BULLETS	= "player bullets";
		public static final String	PLAYER_SHIP		= "player ship";
		public static final String	ENEMY_SHIPS		= "enemy ships";
		public static final String	ENEMY_BULLETS	= "enemy bullets";
	}

	public static final int		NUM_STARS			= 500;
	public static final int		PLAYER_Y			= 150;
	public static final int 	ENEMY_Y_BOTTOM		= 200;
	public static final int		ENEMY_REGION_HEIGHT = (int) (FRAME.getMaxY() - ENEMY_Y_BOTTOM + FRAME.getHeight() / 10);
	public static final String	TEXTURE_ORIGINALS	= "texture-originals";
	public static final String	TEXTURE				= "texture";
	public static final String	PACK_ATLAS			= TEXTURE + "/pack.atlas";

	public static final String	IMG_FIGHTER			= "fighter";
	public static final String	IMG_BULLET			= "bullet";
	public static final String	IMG_PARTICLE		= "particle";
	public static final String	IMG_EXPLOSION		= "explosion";
	public static final String  IMG_STAR			= "star";

	public static final String FONT_NORMAL 			= "fonts/normal.fnt";
	public static final String FONT_HUD				= "fonts/normal.fnt";
	public static final String FONT_IMG_NORMAL		= "fonts/normal_0.png";
	public static final String FONT_IMG_HUD			= "fonts/normal_0.png";

	public static float computeScaleFromY(float y)
	{
		return 1 - ((y - ENEMY_Y_BOTTOM) / (float) ENEMY_REGION_HEIGHT);
	}
}
