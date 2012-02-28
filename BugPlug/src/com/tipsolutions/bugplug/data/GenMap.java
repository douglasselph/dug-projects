package com.tipsolutions.bugplug.data;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.tipsolutions.bugplug.MyApplication;
import com.tipsolutions.jacket.misc.Msg;

public class GenMap {

	class Cell {
	};

	class Cells {
		Cell [] mCells;
		final short mRowSize;

		public Cells(short width, short numrows) {
			mRowSize = width;
			mCells = new Cell[mRowSize*numrows];
		}

		public Cell get(int x, int y) {
			return mCells[y*mRowSize+x];
		}
	};

	final Context mCtx;
	final String mName;;
	Cells mCells;;

	public GenMap(Context context, String name) {
		mCtx = context;
		mName = name;
	}

	public void init() {
		MapXmlData parser = new MapXmlData();
		InputStream is = null;
		try {
			is = mCtx.getAssets().open(mName + "/" + "map.xml");
			parser.parse(is);
		} catch (Exception ex) {
			Log.e(MyApplication.TAG, ex.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
				is = null;
			}
		}
		AssetManager am = mCtx.getResources().getAssets();
		Bitmap bitmap = null;

		try {
			is = am.open(mFilename);
			bitmap = BitmapFactory.decodeStream(is);
		} catch (Exception ex) {
			Msg.err(ex.getMessage());
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch(IOException e) {
				Msg.err(e.getMessage());
			}
		}
	}
}
