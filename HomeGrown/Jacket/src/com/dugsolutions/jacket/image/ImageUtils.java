package com.dugsolutions.jacket.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.dugsolutions.jacket.file.FileUtils;
import com.dugsolutions.jacket.misc.Msg;

public class ImageUtils
{
	static AssetManager	mAM;

	static public Bitmap FlipRows(Bitmap bitmap)
	{
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Bitmap flipped = Bitmap.createBitmap(width, height, bitmap.getConfig());
		for (int r = 0; r < height; r++)
		{
			for (int c = 0; c < width; c++)
			{
				flipped.setPixel(c, height - r - 1, bitmap.getPixel(c, r));
			}
		}
		return flipped;
	}

	static AssetManager getAM(Context context)
	{
		if (mAM == null)
		{
			mAM = context.getResources().getAssets();
		}
		return mAM;
	}

	static public Bitmap LoadBitmap(Context context, int resId)
	{
		Bitmap bitmap = null;
		InputStream is = null;
		try
		{
			is = context.getResources().openRawResource(resId);
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
		}
		catch (Exception ex)
		{
			Msg.err(ex.getMessage());
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
			}
			catch (IOException e)
			{
				Msg.err(e.getMessage());
			}
		}
		return bitmap;

	}

	static public Bitmap LoadBitmap(Context context, String filename)
	{
		Bitmap bitmap = null;
		InputStream is = null;
		try
		{
			is = getAM(context).open(filename);
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
		}
		catch (Exception ex)
		{
			Msg.err(ex.getMessage());
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
			}
			catch (IOException e)
			{
				Msg.err(e.getMessage());
			}
		}
		return bitmap;
	}

	static public void SaveBitmap(Bitmap bitmap, File file) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		fos.flush();
		fos.close();
	}

	static public void SaveBitmap(Context context, Bitmap bitmap, String filename)
	{
		try
		{
			final File file = FileUtils.GetExternalFile(filename, true);
			ImageUtils.SaveBitmap(bitmap, file);
		}
		catch (Exception ex)
		{
			Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
