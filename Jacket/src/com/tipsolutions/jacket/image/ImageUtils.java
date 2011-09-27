package com.tipsolutions.jacket.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.tipsolutions.jacket.file.FileUtils;

public class ImageUtils {

	static public Bitmap FlipRows(Bitmap bitmap) {
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
	
	static public void SaveBitmap(Bitmap bitmap, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		fos.flush();
		fos.close();
	}
	
	static public void SaveBitmap(Context context, Bitmap bitmap, String filename) {
		try {
			final File file = FileUtils.GetExternalFile(filename, true);
			ImageUtils.SaveBitmap(bitmap, file);
		} catch (Exception ex) {
			Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
