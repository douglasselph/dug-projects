package com.dugsolutions.spaceshipwarrior.components;

import com.artemis.Component;

public class SoundEffect extends Component
{
	public enum EFFECT
	{
		PEW, ASPLODE, SMALLASPLODE;

	}

	public EFFECT	effect;

	public SoundEffect(EFFECT e)
	{
		effect = e;
	}
}
