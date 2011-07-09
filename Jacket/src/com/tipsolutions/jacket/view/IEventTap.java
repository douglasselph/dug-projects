package com.tipsolutions.jacket.view;


public interface IEventTap {
	boolean pressDown(float x, float y);
	boolean pressMove(float x, float y);
	boolean pressUp(float x, float y);
}

