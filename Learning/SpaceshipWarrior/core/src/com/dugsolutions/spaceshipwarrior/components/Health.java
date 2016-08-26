package com.dugsolutions.spaceshipwarrior.components;

import com.artemis.Component;

public class Health extends Component
{
	public Health(float health, float maxHealth)
	{
		this.health = health;
		this.maxHealth = maxHealth;
	}

	public Health(float health)
	{
		this(health, health);
	}

	public Health()
	{
		this(0, 0);
	}

	public float	health, maxHealth;
}
