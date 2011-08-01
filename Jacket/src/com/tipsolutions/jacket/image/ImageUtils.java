package com.tipsolutions.jacket.image;

import android.graphics.Bitmap;

public class ImageUtils {

	static public Bitmap flipRows(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
    	Bitmap flipped = Bitmap.createBitmap(width, height, bitmap.getConfig());
    	for (int r = 0; r < height; r++) {
    		for (int c = 0; c < width; c++) {
    			flipped.setPixel(c, height-r-1, bitmap.getPixel(c, r));
    		}
    	}
    	return flipped;
	}
}
