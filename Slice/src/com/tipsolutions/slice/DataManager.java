package com.tipsolutions.slice;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.tipsolutions.jacket.data.Box;
import com.tipsolutions.jacket.data.Pyramid;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.data.ShapeGL;
import com.tipsolutions.jacket.data.Shape.CullFace;
import com.tipsolutions.jacket.data.Shape.dFloatBuf;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.slice.ViewObj.CreateShape;

public class DataManager {

	class Data {
		CreateShape mCreate;
		ShapeGL mShape = null;
		
		Data(CreateShape create) {
			mCreate = create;
		}
		
		ShapeGL getShape() {
			if (mShape == null) {
				mShape = mCreate.create();
			}
			return mShape;
		}
	}
	static final int DATA_PYRAMID = 0;
	static final int DATA_CUBE = 1;
	static final int DATA_BOX = 2;
	static final int DATA_SUSAN = 3;
	static final int DATA_HANK = 4;
	static final int DATA_WING1 = 5;
	static final int DATA_WINGARM = 6;
	static final int DATA_NUM = 7;

    protected Data [] mData = null;
    
    final String CUBE_FILE = "cube.data";
    final String SUSAN_FILE = "suzanne.data";
    final String HANK_FILE = "hank.data";
    final String WING1_FILE = "wingL.data";
    final String WINGARM_FILE = "wingArm.data";
    static final int NUM_FILES = 5;
    
    ShapeGL [] mShapes = new ShapeGL[DATA_NUM];
    Context mCtx;
    MyApplication mApp;
    
    public DataManager(Context context) {
    	mCtx = context;
    	mApp = (MyApplication)context.getApplicationContext();
    }
    
    ShapeGL getBox() {
//      TextureManager.Texture texture = mRenderer.getTextureManager().getTexture(R.raw.robot);
      TextureManager.Texture texture = mApp.getTextureManager().getTexture("feather_real.png");
      final ShapeGL shape = new Box(1f, texture);
      shape.setLocation(new Vector3f(0f, -shape.getBounds().getSizeY()/2, 0));
      shape.setColorData(new dFloatBuf() {
			@Override
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
  
			@Override
			public int size() {
				return shape.getNumVertexes()*4;
			}
      });
      return shape;
  }
    
    ShapeGL getPyramid() {
        TextureManager.Texture texture = mApp.getTextureManager().getTexture(R.raw.robot);
        ShapeGL shape = new Pyramid(1f, 1f, texture);
        shape.setLocation(new Vector3f(0f, -shape.getBounds().getSizeY()/2, 0));
        setColors(shape);
        return shape;
    }
    
    public ShapeGL getShape(int index) {
    	return mData[index].getShape();
    }

    
    public void init() {
        mData = new Data[DATA_NUM];
        
        mData[DATA_PYRAMID] = new Data(new CreateShape() {
			public ShapeGL create() {
				return getPyramid();
			}
        });
        mData[DATA_CUBE] = new Data(new CreateShape() {
			public ShapeGL create() {
				return loadShape(CUBE_FILE);
			}
        });
        mData[DATA_BOX] = new Data(new CreateShape() {
			public ShapeGL create() {
				return getBox();
			}
        });
        mData[DATA_SUSAN] = new Data(new CreateShape() {
			public ShapeGL create() {
				ShapeGL shape = loadShape(SUSAN_FILE);
		        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
				setColors(shape);
				return shape;
			}
        });
        mData[DATA_HANK] = new Data(new CreateShape() {
			public ShapeGL create() {
				ShapeGL shape = loadShape(HANK_FILE);
				setColors(shape);
				return shape;
			}
        });
        mData[DATA_WING1] = new Data(new CreateShape() {
			public ShapeGL create() {
				return loadShape(WING1_FILE);
			}
        });
        mData[DATA_WINGARM] = new Data(new CreateShape() {
			public ShapeGL create() {
				ShapeGL shape = loadShape(WINGARM_FILE);
				shape.setCullFace(CullFace.NONE);
				return shape;
			}
        });
    }
  
    public boolean initialized() {
    	return (mData != null);
    }
    
    ShapeGL loadShape(String file) {
        ShapeGL shape = new ShapeGL();
        try {
            InputStream inputStream = mCtx.getAssets().open(file);
            shape.readData(inputStream, mApp.getTextureManager());
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
        return shape;
    }
    
	void setColors(final Shape shape) {
    	if (shape.getNumVertexes() > 0) {
    //        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
            shape.setColorData(new dFloatBuf() {
    			@Override
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
        
    			@Override
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
