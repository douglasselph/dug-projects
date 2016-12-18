package com.dugsolutions.crawl.components;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SpriteAnimation extends Component
{
	public Animation			animation;
	public float				stateTime;
	public float				frameDuration;
	public Animation.PlayMode	playMode;

	public TextureRegion getFrame()
	{
		return animation.getKeyFrame(stateTime);
	}

}