package com.tipsolutions.slice;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.shape.Box;
import com.tipsolutions.jacket.shape.Pyramid;
import com.tipsolutions.jacket.shape.Shape;
import com.tipsolutions.jacket.shape.BufferUtils.dFloatBuf;
import com.tipsolutions.jacket.shape.Shape.CullFace;

public class DataManager {

//	class Data {
//		CreateShape mCreate;
//		Shape mShape = null;
//		
//		Data(CreateShape create) {
//			mCreate = create;
//		}
//		
//		Shape getShape() {
//			if (mShape == null) {
//				mShape = mCreate.create();
//			}
//			return mShape;
//		}
//	}
	static final int DATA_PYRAMID = 0;
	static final int DATA_CUBE = 1;
	static final int DATA_BOX = 2;
	static final int DATA_SUSAN = 3;
	static final int DATA_HANK = 4;
	static final int DATA_WING1 = 5;
	static final int DATA_WINGARM = 6;
	static final int DATA_NUM = 7;

//    protected Data [] mData = null;
    
    final String CUBE_FILE = "cube.data";
    final String SUSAN_FILE = "suzanne.data";
    final String HANK_FILE = "hank.data";
    final String WING1_FILE = "wingL.data";
    final String WINGARM_FILE = "wingArm.data";
    static final int NUM_FILES = 5;
    
//    Shape [] mShapes = new Shape[DATA_NUM];
    Context mCtx;
    MyApplication mApp;
    TextureManager mTM;
    
    public DataManager(Context context) {
    	mCtx = context;
    	mApp = (MyApplication)context.getApplicationContext();
    }
    
    TextureManager.Texture getTexture(String file) {
    	if (mTM == null) {
    		return null;
    	}
    	return mTM.getTexture(file);
    }
    
    TextureManager.Texture getTexture(int resid) {
    	if (mTM == null) {
    		return null;
    	}
    	return mTM.getTexture(resid);
    }
    
    Shape getBox() {
      TextureManager.Texture texture = getTexture("feather_real.png");
      final Shape shape = new Box(1f, texture);
      shape.setLocation(new Vector3f(0f, -shape.getBounds().getSizeY()/2, 0));
      shape.setColorData(new dFloatBuf() {
			public void fill(FloatBuffer buf) {
				ArrayList<Color4f> colors = new ArrayList<Color4f>();
				colors.add(new Color4f(1, 0, 0, 1));
				colors.add(new Color4f(0, 1, 0, 1));
				colors.add(new Color4f(0, 0, 1, 1));
				colors.add(new Color4f(1, 0, 1, 1));
				colors.add(new Color4f(1, 1, 0, 1));
				colors.add(new Color4f(0.5f, 0.5f, 0.5f, 1));
				int next = 0;
				int count = 0;
				for (int i = 0; i < shape.getNumVertexes(); i++) {
					colors.get(next).put(buf);
					
					if (++count >= 4) {
						count = 0;
						if (++next >= colors.size()) {
							next = 0;
						}
					}
				}
			}
  
			public int size() {
				return shape.getNumVertexes()*4;
			}
      });
      return shape;
  }
    
    Shape getPyramid() {
        TextureManager.Texture texture = getTexture(R.raw.robot);
        Shape shape = new Pyramid(1f, 1f, texture);
        shape.setLocation(new Vector3f(0f, -shape.getBounds().getSizeY()/2, 0));
        setColors(shape);
        return shape;
    }
    
    public Shape getShape(int index) {
    	switch (index) {
    		case DATA_PYRAMID:
				return getPyramid();
    		case DATA_CUBE:
				return loadShape(CUBE_FILE);
    		case DATA_BOX:
				return getBox();
    		case DATA_SUSAN:
    		{
				Shape shape = loadShape(SUSAN_FILE);
		        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
				setColors(shape);
				return shape;
    		}
    		case DATA_HANK:
    		{
				Shape shape = loadShape(HANK_FILE);
				setColors(shape);
				return shape;
    		}
    		case DATA_WING1:
				return loadShape(WING1_FILE);
    		case DATA_WINGARM:
    		{
				Shape shape = loadShape(WINGARM_FILE);
				shape.setCullFace(CullFace.NONE);
				return shape;
    		}
    	}
    	return null;
    }

    public void init(TextureManager tm) {
//        mData = new Data[DATA_NUM];
        mTM = tm;
        
//        mData[DATA_PYRAMID] = new Data(new CreateShape() {
//			public Shape create() {
//				return getPyramid();
//			}
//        });
//        mData[DATA_CUBE] = new Data(new CreateShape() {
//			public Shape create() {
//				return loadShape(CUBE_FILE);
//			}
//        });
//        mData[DATA_BOX] = new Data(new CreateShape() {
//			public Shape create() {
//				return getBox();
//			}
//        });
//        mData[DATA_SUSAN] = new Data(new CreateShape() {
//			public Shape create() {
//				Shape shape = loadShape(SUSAN_FILE);
//		        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
//				setColors(shape);
//				return shape;
//			}
//        });
//        mData[DATA_HANK] = new Data(new CreateShape() {
//			public Shape create() {
//				Shape shape = loadShape(HANK_FILE);
//				setColors(shape);
//				return shape;
//			}
//        });
//        mData[DATA_WING1] = new Data(new CreateShape() {
//			public Shape create() {
//				return loadShape(WING1_FILE);
//			}
//        });
//        mData[DATA_WINGARM] = new Data(new CreateShape() {
//			public Shape create() {
//				Shape shape = loadShape(WINGARM_FILE);
//				shape.setCullFace(CullFace.NONE);
//				return shape;
//			}
//        });
    }
  
    public boolean initialized() {
    	return (mTM != null);
    }
    
    Shape loadShape(String file) {
        Shape shape = new Shape();
        try {
            InputStream inputStream = mCtx.getAssets().open(file);
            shape.readData(inputStream, mTM);
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
        return shape;
    }
    
	void setColors(final Shape shape) {
    	if (shape.getNumVertexes() > 0) {
    //        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
            shape.setColorData(new dFloatBuf() {
    			public void fill(FloatBuffer buf) {
    				ArrayList<Color4f> colors = new ArrayList<Color4f>();
    				colors.add(new Color4f(1, 0, 0, 1));
    				colors.add(new Color4f(0, 1, 0, 1));
    				colors.add(new Color4f(0, 0, 1, 1));
    				colors.add(new Color4f(1, 0, 1, 1));
    				colors.add(new Color4f(0, 1, 1, 1));
    				colors.add(new Color4f(0.5f, 0.5f, 0.5f, 1));
    				int next = 0;
    				for (int i = 0; i < shape.getNumVertexes(); i++) {
    					colors.get(next).put(buf);
    					if (++next >= colors.size()) {
    						next = 0;
    					}
    				}
    			}
        
    			public int size() {
    				return shape.getNumVertexes()*4;
    			}
            });
    	} else {
    		for (Shape child : shape.getChildren()) {
    			setColors(child);
    		}
    	}
    }
	
}
