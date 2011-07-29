package com.tipsolutions.jacket.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;

import com.tipsolutions.jacket.math.Constants;
import com.tipsolutions.jacket.math.MatrixTrackingGL;

public class TextureManager {
	
	public class Texture {
		int mTextureID = 0;
		int mUseCount = 0;
		
		public Texture() {
		}
		
		public void done() {
			mUseCount--;
		}
		
		public boolean used() {
			return mUseCount > 0;
		}
		
		public boolean initialized() {
			return mTextureID != 0;
		}
		
		void init(InputStream is, MatrixTrackingGL gl) {
			int[] textures = new int[1];
			gl.glGenTextures(1, textures, 0);
			mTextureID = textures[0];
			
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D,
	                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

	        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, 
	        		GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
	        
	        Bitmap bitmap = null;
	        try {
	            bitmap = BitmapFactory.decodeStream(is);
	        } catch (Exception ex) {
	        	Log.e(Constants.TAG, ex.getMessage());
	        }
	        if (bitmap != null) {
	            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
	            bitmap.recycle();
	        }
		}
		
		public void onDraw(MatrixTrackingGL gl) {
			gl.glEnable(GL10.GL_TEXTURE_2D); 
			gl.glTexEnvx(GL10.GL_TEXTURE_ENV, 
					GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		       
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		}
		
		public void use() {
			mUseCount++;
		}
	};
	final Context mContext;
	HashMap<Integer,Texture> mIMap = new HashMap<Integer,Texture>();
	HashMap<String,Texture> mSMap = new HashMap<String,Texture>();
	
	public TextureManager() {
		mContext = null;
	}
	
	public TextureManager(Context ctx) {
		mContext = ctx;
	}
	
	public Texture getTexture(int resId) {
		Texture entry;
		if (mIMap.containsKey(resId)) {
			entry = mIMap.get(resId);
		} else {
			mIMap.put(resId, entry = new Texture());
		}
		return entry;
	}
	
	public Texture getTexture(String filename) {
		Texture entry;
		if (mSMap.containsKey(filename)) {
			entry = mSMap.get(filename);
		} else {
			mSMap.put(filename, entry = new Texture());
		}
		return entry;
	}
	
	public void init(MatrixTrackingGL gl) {
		gl.glEnable(GL10.GL_TEXTURE_2D); 
		
		boolean once = false;
		
		AssetManager am = mContext.getResources().getAssets();
		Texture tex;
		for (String filename : mSMap.keySet()) {
			InputStream is = null;
			try {
				tex = mSMap.get(filename);
				if (!tex.initialized()) {
					is = am.open(filename);
					if (tex.used()) {
						tex.init(is, gl);
					}
				}
				once = true;
			} catch (Exception ex) {
				Log.e(Constants.TAG, ex.getMessage());
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch(IOException e) {
					Log.e(Constants.TAG, e.getMessage());
				}
			}
		}
		for (int resId : mIMap.keySet()) {
			InputStream is = null;
			try {
				tex = mIMap.get(resId);
				if (!tex.initialized()) {
					is = mContext.getResources().openRawResource(resId);
					if (tex.used()) {
						tex.init(is, gl);
					}
				}
				once = true;
			} catch (Exception ex) {
				Log.e(Constants.TAG, ex.getMessage());
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch(IOException e) {
					Log.e(Constants.TAG, e.getMessage());
				}
			}
		}
		if (!once) {
			gl.glDisable(GL10.GL_TEXTURE_2D); 
		}
	}
}
