package com.tipsolutions.jacket.model;

import com.tipsolutions.jacket.image.TextureManager.Texture;

public class Box extends Model {

	public Box() {
		this(1f);
		
	}

	public Box(float length) {
		this(length, length, length, null);
	}
	
	public Box(float length, Texture texture) {
		this(length, length, length, texture);
	}
	
	public Box(float xlength, float ylength, float zlength) {
		this(xlength, ylength, zlength, null);
	}
	
	public Box(float xlength, float ylength, float zlength, Texture texture) {
		
	}
}
