package com.tipsolutions.jacket.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import com.tipsolutions.jacket.math.MatrixTrackingGL;
import com.tipsolutions.jacket.misc.Msg;

public class TextureManager {
	
	int mDefaultBlendParam = GL10.GL_MODULATE;
	int mDefaultBlendSource = GL10.GL_ONE;
	int mDefaultBlendDest = GL10.GL_ONE_MINUS_SRC_ALPHA;
	
	public class Texture {
		final String mFilename;
		final int mResId;
		
		int mTextureID = 0;
		int mBlendParam = mDefaultBlendParam;
		int mBlendSource = mDefaultBlendSource;
		int mBlendDest = mDefaultBlendDest;
		
		public Texture(String filename) {
			mFilename = filename;
			mResId = 0;
		}
		
		public Texture(int resId) {
			mFilename = null;
			mResId = resId;
		}
		
		public Texture(final Texture from) {
			mFilename = from.mFilename;
			mResId = from.mResId;
			mTextureID = from.mTextureID;
    		mBlendParam = from.mBlendParam;
    		mBlendSource = from.mBlendSource;
    		mBlendDest = from.mBlendDest;
		}
		
		public boolean initialized() {
			return mTextureID != 0;
		}
		
		void init(MatrixTrackingGL gl) throws IOException {
			if (!initialized()) {
    			InputStream is = null;
    			try {
    				if (mFilename != null) {
    					is = mAM.open(mFilename);
    				} else {
    					is = mContext.getResources().openRawResource(mResId);
    				}
    				init(is, gl);
    			} catch (Exception ex) {
    				if (mFilename != null) {
    					throw new IOException(Msg.build
        					("File: \"", mFilename, "\", got exception: ", ex.getMessage()));
    				} else {
    					Msg.err(ex.getMessage());
    				}
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
		
		void init(InputStream is, MatrixTrackingGL gl) {
			int[] textures = new int[1];
			gl.glGenTextures(1, textures, 0);
			mTextureID = textures[0];
			
			gl.glEnable(GL10.GL_TEXTURE_2D); 
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D,
	                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, 
	        		GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
	        
	        Bitmap bitmap = null;
	        try {
	            bitmap = BitmapFactory.decodeStream(is);
	        } catch (Exception ex) {
	        	Msg.err(ex.getMessage());
	        }
	        if (bitmap != null) {
	        	Bitmap flipped = ImageUtils.FlipRows(bitmap);
	            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, flipped, 0);
	            bitmap.recycle();
	            flipped.recycle();
	        }
		}
		
		// If a texture is shared across multiple shapes, this alone is called
		public void onDraw(MatrixTrackingGL gl, FloatBuffer fbuf) {
			if (!initialized()) {
				try {
    				init(gl);
				} catch (Exception ex) {
					Msg.err(ex.getMessage());
					return;
				}
			}
			gl.glEnable(GL10.GL_BLEND); 
			gl.glBlendFunc(mBlendSource, mBlendDest);
			
			gl.glEnable(GL10.GL_TEXTURE_2D); 
			gl.glTexEnvx(GL10.GL_TEXTURE_ENV,
					GL10.GL_TEXTURE_ENV_MODE, mBlendParam);
		       
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fbuf);
		}
			
		// Expected to be one of GL10.GL_MODULATE, GL10.GL_DECAL, 
		//   GL10.GL_BLEND, or GL10.GL_REPLACE;
		public void setBlendEnv(int param) {
			mBlendParam = param;
		}
		
		// Expected to be GL10.GL_ONE, 
		// 				  GL10.GL_ONE_MINUS_SRC_ALPHA, etc.
		public void setBlendFunc(int src, int dest) {
			mBlendSource = src;
			mBlendDest = dest;
		}
	};
	
	final Context mContext;
	HashMap<Long,Texture> mMap = new HashMap<Long,Texture>();
	AssetManager mAM;
	
	public TextureManager() {
		mContext = null;
	}
	
	public TextureManager(Context ctx) {
		mContext = ctx;
		mAM = mContext.getResources().getAssets();
	}
	
	public void reset() {
		mMap = new HashMap<Long,Texture>();
	}
	
	public Texture getTexture(int resId) {
		Texture entry;
		long key = (1<<16) + resId;
		if (mMap.containsKey(key)) {
			entry = mMap.get(key);
		} else {
			mMap.put(key, entry = new Texture(resId));
		}
		return entry;
	}
	
	public Texture getTexture(String filename) {
		Texture entry;
		long key = filename.hashCode();
		if (mMap.containsKey(key)) {
			entry = mMap.get(key);
		} else {
			mMap.put(key, entry = new Texture(filename));
		}
		return entry;
	}
	
	public Collection<Texture> getTextures() {
		return mMap.values();
	}
	
	public void init(MatrixTrackingGL gl) throws IOException {
		for (Texture tex : getTextures()){
			tex.init(gl);
		}
	}
	
	public void setDefaultBlendParam(int param) {
		mDefaultBlendParam = param;
	}
	
	public void setBlendParam(int param) {
		setDefaultBlendParam(param);
        for (Texture t : getTextures()) { 
        	t.setBlendEnv(param);
        }
	}
}
