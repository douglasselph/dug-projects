package com.tipsolutions.slice;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.app.Activity;
import android.os.Bundle;

import com.tipsolutions.jacket.data.Pyramid;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.data.ShapeData.FloatData;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Rotate;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.IEventTap;

public class Main extends Activity {
	
	public static final boolean LOG = true;
	public static final String TAG = "Slice";
	
    ControlSurfaceView mSurfaceView;
    ControlCamera mCamera;
    MyRenderer mRenderer;
    final String FigureFile = "cube.data";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        ShapeData shape = new ShapeData();
//        try {
//            InputStream inputStream = getAssets().open(FigureFile);
//            shape.readData(inputStream);
//        } catch (Exception ex) {
//        	Log.e(MyApplication.TAG, ex.getMessage());
//        }
        
//        final Triangle shape  = new Triangle();
        
        final Pyramid shape = new Pyramid(1f, 1f);
        shape.setLocation(new Vector3f(0f, -0.5f, 0));
        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
        shape.setColorData(new FloatData() {
			@Override
			public void fill(FloatBuffer buf) {
				buf.put(1f).put(0f).put(0f).put(1f);
				buf.put(0f).put(1f).put(0f).put(1f);
				buf.put(0f).put(0f).put(1f).put(1f);
				buf.put(0.5f).put(0.5f).put(0.5f).put(1f);
			}

			@Override
			public int size() {
				return 4*4;
			}
        });
        shape.setRotate(new Rotate(0f, 0f, 0f));
        
        mCamera = new ControlCamera();
        mSurfaceView = new ControlSurfaceView(this);
        mRenderer = new MyRenderer(mSurfaceView, shape, mCamera, false);
        mRenderer.setClippingPlaneColor(new Color4f(0.5f, 1.0f, 1.0f));
        
        mSurfaceView.setRenderer(mRenderer);
        
        mSurfaceView.setEventTap(mCamera);
//        ObjEventTap eventTap = new ObjEventTap(shape);
//        mSurfaceView.setEventTap(eventTap);
        
        setContentView(mSurfaceView);
        
//        mSurfaceView.requestFocus();
//        mSurfaceView.setFocusableInTouchMode(true);
    }
    
	
    
    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }
   
    class Triangle extends Shape {
    	
    	Triangle() {
    		fill();
    	}
    	
    	@Override protected float _getMinX() { return -0.500000f; }
    	@Override protected float _getMaxX() { return 0.500000f; }
    	@Override protected float _getMinY() { return -0.500001f; }
    	@Override protected float _getMaxY() { return 0.500000f; }
    	@Override protected float _getMinZ() { return 0.000000f; }
    	@Override protected float _getMaxZ() { return 0.000000f; }
    	@Override protected Color4f _getColor4() { return new Color4f(0.5f, 0f, 0f, 0.5f); }

    	@Override
    	protected FloatData getVertexData() {
    		return new FloatData() {
    			public void fill(FloatBuffer buf) {
    				buf.put(-0.500000f).put(-0.500000f).put(0.000000f); /* 0 */
    				buf.put(0.500000f).put(-0.500000f).put(0.000000f); /* 1 */
    				buf.put(0.000000f).put(0.500000f).put(0.000000f); /* 2 */
    			};
    			public int size() { return 3*3; }
    		};
    	};

    	@Override
    	protected ShortData getIndexData() {
    		return new ShortData() {
    			public void fill(ShortBuffer buf) {
    				buf.put((short)0).put((short)1).put((short)2); /* 0 */
    			};
    			public int size() { return 3; }
    		};
    	};
    	
    	@Override
    	protected FloatData getColorData() {
    		return new FloatData() {
    			public void fill(FloatBuffer buf) {
    				buf.put(1f).put(0f).put(0).put(1f); /* 0 */
    				buf.put(0f).put(1f).put(0).put(1f); /* 0 */
    				buf.put(0f).put(0f).put(1f).put(1f); /* 0 */
    			};
    			public int size() { return 3*4; }
    		};
    	};
    	
    };
   
    class ObjEventTap implements IEventTap {
    	
     	float _x;
    	float _y;
    	final Shape mShape;
    	
    	ObjEventTap(Shape shape){
    		mShape = shape;
    	}
    	
    	public boolean pressDown(final float x, final float y) {
    		_x = x;
    		_y = y;
    		mSurfaceView.queueEvent(new Runnable() {
    			public void run() {
            		mRenderer.setClippingPlaneColor(
        				new Color4f(x / mSurfaceView.getWidth(), y / mSurfaceView.getHeight(), 1f));
    			}
    		});
    		return true;
    	}
    	
    	public boolean pressMove(final float x, final float y) {
    		mSurfaceView.queueEvent(new Runnable() {
    			public void run() {
    				float xdiff = (_x - x);
    				float ydiff = (_y - y);
    				mShape.addRotate(new Rotate(ydiff, xdiff, 0f));
    				mSurfaceView.requestRender();
    			}
    		});
    		return true;
    	}
    	
    	public boolean pressUp(float x, float y){
    		return false;
    	}
    }
    
}