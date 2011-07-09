package com.tipsolutions.jacket.view;

public interface IControl {
	void sideLeft();
	void sideRight();
	void sideUp();
	void sideDown();
	void centerLong();
	void centerShort();
	void slideLeft(int times);
	void slideRight(int times);
	void slideUp(int times);
	void slideDown(int times);
	void touchStart();
	void touchEnd();
}
