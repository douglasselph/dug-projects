package com.tipsolutions.slice;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.tipsolutions.jacket.data.Pyramid;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.data.ShapeData.FloatData;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.IEventTap;

public class Main extends Activity {

	static final int MENU_PYRAMID = 0;
	static final int MENU_CUBE = 1;
	static final int MENU_SUSAN = 2;
	static final int MENU_HANK = 3;
	static final int MENU_PIGEON = 4;
	
	final int DATA_PYRAMID = 0;
	final int DATA_CUBE = 1;
	final int DATA_SUSAN = 2;
	final int DATA_HANK = 3;
	final int DATA_PIGEON = 4;
	
	interface CreateShape {
		Shape create();
	}
	
	class Data {
		CreateShape mCreate;
		Shape mShape = null;
		
		Data(CreateShape create) {
			mCreate = create;
		}
		
		Shape getShape() {
			if (mShape == null) {
				mShape = mCreate.create();
			}
			return mShape;
		}
	};
	
	Data [] mData;
	
//	class Triangle extends Shape {
//    	
//    	Triangle() {
//    		fill();
//    	}
//    	
//    	@Override protected Color4f _getColor4() { return new Color4f(0.5f, 0f, 0f, 0.5f); }
//    	@Override protected float _getMaxX() { return 0.500000f; }
//    	@Override protected float _getMaxY() { return 0.500000f; }
//    	@Override protected float _getMaxZ() { return 0.000000f; }
//    	@Override protected float _getMinX() { return -0.500000f; }
//    	@Override protected float _getMinY() { return -0.500001f; }
//    	@Override protected float _getMinZ() { return 0.000000f; }
//
//    	@Override
//    	protected FloatData getColorData() {
//    		return new FloatData() {
//    			public void fill(FloatBuffer buf) {
//    				buf.put(1f).put(0f).put(0).put(1f); /* 0 */
//    				buf.put(0f).put(1f).put(0).put(1f); /* 0 */
//    				buf.put(0f).put(0f).put(1f).put(1f); /* 0 */
//    			};
//    			public int size() { return 3*4; }
//    		};
//    	};
//
//    	@Override
//    	protected ShortData getIndexData() {
//    		return new ShortData() {
//    			public void fill(ShortBuffer buf) {
//    				buf.put((short)0).put((short)1).put((short)2); /* 0 */
//    			};
//    			public int size() { return 3; }
//    		};
//    	};
//    	
//    	@Override
//    	protected FloatData getVertexData() {
//    		return new FloatData() {
//    			public void fill(FloatBuffer buf) {
//    				buf.put(-0.500000f).put(-0.500000f).put(0.000000f); /* 0 */
//    				buf.put(0.500000f).put(-0.500000f).put(0.000000f); /* 1 */
//    				buf.put(0.000000f).put(0.500000f).put(0.000000f); /* 2 */
//    			};
//    			public int size() { return 3*3; }
//    		};
//    	};
//    }
	
	class TwirlEventTap implements IEventTap {
    
    	static final short DOUBLE_TAP_TRIGGER_MS = 400;
    	float _x;
    	float _y;
     	long mLastTouchTime;
    	long mStartTouchTime;
   	
    	TwirlEventTap() {
    	}
    	
    	public boolean pressDown(final float x, final float y) {
    		_x = x;
    		_y = y;
    		mStartTouchTime = System.currentTimeMillis();
//    		mSurfaceView.queueEvent(new Runnable() {
//    			public void run() {
//            		mRenderer.setClippingPlaneColor(
//        				new Color4f(x / mSurfaceView.getWidth(), y / mSurfaceView.getHeight(), 1f));
//    			}
//    		});
    		return true;
    	}
    	
    	public boolean pressMove(final float x, final float y) {
    		mSurfaceView.queueEvent(new Runnable() {
    			public void run() {
    				float xdiff = (_x - x);
    				float ydiff = (_y - y);
    				mActiveShape.addRotate(ydiff, xdiff, 0f);
    				mSurfaceView.requestRender();
    			}
    		});
    		return true;
    	}
    	
    	public boolean pressUp(float x, float y){
    		long curTime = System.currentTimeMillis();
    		long diffTime = curTime - mStartTouchTime;
    		if (diffTime <= DOUBLE_TAP_TRIGGER_MS) {
        		diffTime = mStartTouchTime - mLastTouchTime;
        		if (diffTime <= DOUBLE_TAP_TRIGGER_MS) {
    				mSurfaceView.setEventTap(mCamera);
    			}
    		}
    		mLastTouchTime = curTime;
    		return false;
    	}
    }
	
    public static final boolean LOG = true;
    public static final String TAG = "Slice";
    final String CUBE_FILE = "cube.data";
    final String SUSAN_FILE = "susan.data";
    final String HANK_FILE = "hank.data";
    final String PIGEON_FILE = "pigeon.data";
    
    static final int NUM_FILES = 4;
    static final int ARRAY_SIZE = NUM_FILES+1;
    
    Shape [] mShapes = new Shape[ARRAY_SIZE];
    Shape mActiveShape = null;
    TwirlEventTap mTwirlEventTap = new TwirlEventTap();
    ControlCamera mCamera;
    MyRenderer mRenderer;
    ControlSurfaceView mSurfaceView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        final Triangle shape  = new Triangle();
        
        mData = new Data[ARRAY_SIZE];
        mData[DATA_PYRAMID] = new Data(new CreateShape() {
			public Shape create() {
				return getPyramid();
			}
        });
        mData[DATA_CUBE] = new Data(new CreateShape() {
			public Shape create() {
				Shape shape = loadShape(CUBE_FILE);
				setColors(shape);
				return shape;
			}
        });
        mData[DATA_SUSAN] = new Data(new CreateShape() {
			public Shape create() {
				Shape shape = loadShape(SUSAN_FILE);
		        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
				setColors(shape);
				return shape;
			}
        });
        mData[DATA_HANK] = new Data(new CreateShape() {
			public Shape create() {
				Shape shape = loadShape(HANK_FILE);
				setColors(shape);
				return shape;
			}
        });
        mData[DATA_PIGEON] = new Data(new CreateShape() {
			public Shape create() {
				Shape shape = loadShape(PIGEON_FILE);
		        shape.setColor(new Color4f(0.4f, 0.3f, 0.4f, 0.5f));
				return shape;
			}
        });
        
        mCamera = new ControlCamera();
        mSurfaceView = new ControlSurfaceView(this);
        mRenderer = new MyRenderer(mSurfaceView, null, mCamera, false);
        mRenderer.setClippingPlaneColor(new Color4f(0.5f, 1.0f, 1.0f));
        mSurfaceView.setRenderer(mRenderer);
        
        mCamera.setDoubleTap(new Runnable() {
			@Override
			public void run() {
				mSurfaceView.setEventTap(mTwirlEventTap);
			}
        });
        setShape(getPyramid());
        
        setContentView(mSurfaceView);
        
//        mSurfaceView.requestFocus();
//        mSurfaceView.setFocusableInTouchMode(true);
        
        testMatrixAddRotate();
    }
    
    void setShape(Shape shape) {
    	mActiveShape = shape;
        mRenderer.setShape(shape);
        
    	mCamera.setLookAt(new Vector3f(0, 0, 0));
    	mCamera.setLocation(mCamera.getLookAt().dup());
    	mCamera.getLocation().add(0, 0, mActiveShape.getSizeZc()*2);
        
        mSurfaceView.setEventTap(mCamera);
        mSurfaceView.requestRender();
    }
   
    Shape loadShape(String file) {
        Shape shape = new Shape();
        try {
            InputStream inputStream = getAssets().open(file);
            shape.readData(inputStream);
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
        return shape;
    }
    
    Shape getPyramid() {
        Shape shape = new Pyramid(1f, 1f);
        shape.setLocation(new Vector3f(0f, -shape.getSizeYc()/2, 0));
        setColors(shape);
        return shape;
    }
    
    void setColors(Shape shape) {
//        shape.setColor(new Color4f(0.5f, 0f, 0f, 0.5f));
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
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    };
   
    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_PYRAMID, 0, "Pyramid");
		menu.add(0, MENU_CUBE, 0, "Cube");
		menu.add(0, MENU_SUSAN, 0, "Susan");
		menu.add(0, MENU_HANK, 0, "Hank");
		menu.add(0, MENU_PIGEON, 0, "Pigeon");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
    		case MENU_PYRAMID:
    			setShape(mData[DATA_PYRAMID].getShape());
    			break;
    		case MENU_CUBE:
    			setShape(mData[DATA_CUBE].getShape());
    			break;
    		case MENU_SUSAN:
    			setShape(mData[DATA_SUSAN].getShape());
    			break;
    		case MENU_HANK:
    			setShape(mData[DATA_HANK].getShape());
    			break;
    		case MENU_PIGEON:
    			setShape(mData[DATA_PIGEON].getShape());
    			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	void testMatrixAddRotate() {
		
		class Test {
			String mName;
			Vector3f mVecInit;
			Vector3f mVecPost;
			int mRotX, mRotY, mRotZ;
			
			Test(String name, int rx, int ry, int rz, Vector3f initVec, Vector3f postVec) {
				mName = name;
				mRotX = rx;
				mRotY = ry;
				mRotZ = rz;
				mVecInit = initVec;
				mVecPost = postVec;
			}
			
			float run() {
				float diff = 0;
				Quaternion quat = new Quaternion();
				quat.fromAngles(Math.toRadians(mRotX), Math.toRadians(mRotY), Math.toRadians(mRotZ));
				
				Vector3f vec = new Vector3f(quat.apply(mVecInit, null));
				diff += assertEquals(mName, mVecPost, vec);
				
				return diff;
			}
			
			Vector3f getExpected() {
				Matrix3f m = new Matrix3f();
				m.setRotate(mRotX, mRotY, mRotY);
				return m.apply(mVecInit,null);
			}
		};
		double cos45 = Math.cos(Math.toRadians(45));
		float cos45f = (float) cos45;
		double cos190 = Math.cos(Math.toRadians(190));
		float cos190f = (float) cos190;
		double sin190 = Math.sin(Math.toRadians(190));
		float sin190f = (float) sin190;
		
		ArrayList<Test> tests = new ArrayList<Test>();
		tests.add(new Test("Test01", 0, 0, 0, new Vector3f(1,0,0), new Vector3f(1,0,0)));
		tests.add(new Test("Test02", 45, 0, 0, new Vector3f(1,0,0), new Vector3f(1,0,0)));
		tests.add(new Test("Test03", 45, 0, 0, new Vector3f(0,1,0), new Vector3f(0,cos45f,cos45f)));
		tests.add(new Test("Test04", 45, 0, 0, new Vector3f(0,0,1), new Vector3f(0,-cos45f,cos45f)));
		tests.add(new Test("Test05", 0, 45, 0, new Vector3f(1,0,0), new Vector3f(cos45f,0,-cos45f)));
		tests.add(new Test("Test06", 0, 45, 0, new Vector3f(0,1,0), new Vector3f(0,1,0)));
		tests.add(new Test("Test07", 0, 45, 0, new Vector3f(0,0,1), new Vector3f(cos45f,0,cos45f)));
		tests.add(new Test("Test08", 0, 0, 45, new Vector3f(1,0,0), new Vector3f(cos45f,cos45f,0)));
		tests.add(new Test("Test09", 0, 0, 45, new Vector3f(0,1,0), new Vector3f(-cos45f,cos45f,0)));
		tests.add(new Test("Test10", 0, 0, 45, new Vector3f(0,0,1), new Vector3f(0,0,1)));
		tests.add(new Test("Test11", -45, 0, 0, new Vector3f(1,1,0), new Vector3f(1,cos45f,-cos45f)));
		tests.add(new Test("Test12", 0, -45, 0, new Vector3f(1,1,0), new Vector3f(cos45f,1,cos45f)));
		tests.add(new Test("Test13", 0, 0, -45, new Vector3f(1,1,0), new Vector3f(2*cos45f,0,0)));
		tests.add(new Test("Test14", 315, 0, 0, new Vector3f(1,1,0), new Vector3f(1,cos45f,-cos45f)));
		tests.add(new Test("Test15", 0, 315, 0, new Vector3f(1,1,0), new Vector3f(cos45f,1,cos45f)));
		tests.add(new Test("Test16", 0, 0, 315, new Vector3f(1,1,0), new Vector3f(2*cos45f,0,0)));
		tests.add(new Test("Test17", 45, 45, 0, new Vector3f(1,0,0), new Vector3f(cos45f,0.5f,-0.5f)));
		tests.add(new Test("Test18", 45, 45, 0, new Vector3f(0,1,0), new Vector3f(0,cos45f,cos45f)));
		tests.add(new Test("Test19", 45, 45, 0, new Vector3f(0,0,1), new Vector3f(cos45f,-.5f,0.5f)));
		tests.add(new Test("Test20", -45, 45, 0, new Vector3f(1,0,0), new Vector3f(cos45f,-0.5f,-0.5f)));
		tests.add(new Test("Test21", -45, 45, 0, new Vector3f(0,1,0), new Vector3f(0,cos45f,-cos45f)));
		tests.add(new Test("Test22", -45, 45, 0, new Vector3f(0,0,1), new Vector3f(cos45f,.5f,0.5f)));
		tests.add(new Test("Test13", 315, 45, 0, new Vector3f(1,0,0), new Vector3f(cos45f,-0.5f,-0.5f)));
		tests.add(new Test("Test24", 315, 45, 0, new Vector3f(0,1,0), new Vector3f(0,cos45f,-cos45f)));
		tests.add(new Test("Test25", 315, 45, 0, new Vector3f(0,0,1), new Vector3f(cos45f,.5f,0.5f)));
		tests.add(new Test("Test26", 45, 0, -45, new Vector3f(1,0,0), new Vector3f(cos45f,-0.5f,-0.5f)));
		tests.add(new Test("Test27", 45, 0, -45, new Vector3f(0,1,0), new Vector3f(cos45f,0.5f,0.5f)));
		tests.add(new Test("Test28", 45, 0, -45, new Vector3f(0,0,1), new Vector3f(0,-cos45f,cos45f)));
		tests.add(new Test("Test29", 45, 0, -45, new Vector3f(0,1,1), new Vector3f(cos45f,-cos45f+.5f,cos45f+.5f)));
		tests.add(new Test("Test30", 45, 0, -45, new Vector3f(1,0,1), new Vector3f(cos45f,-cos45f-.5f,cos45f-.5f)));
		tests.add(new Test("Test31", 45, 0, -45, new Vector3f(1,1,0), new Vector3f(cos45f*2,0,0)));
		tests.add(new Test("Test32", 45, 0, -45, new Vector3f(1,1,1), new Vector3f(cos45f*2,-cos45f,cos45f)));
		tests.add(new Test("Test33", 45, 0, 315, new Vector3f(1,0,0), new Vector3f(cos45f,-0.5f,-0.5f)));
		tests.add(new Test("Test34", 45, 0, 315, new Vector3f(0,1,0), new Vector3f(cos45f,0.5f,0.5f)));
		tests.add(new Test("Test35", 45, 0, 315, new Vector3f(0,0,1), new Vector3f(0,-cos45f,cos45f)));
		tests.add(new Test("Test36", 190, 0, 0, new Vector3f(1,0,0), new Vector3f(1,0,0)));
		tests.add(new Test("Test37", 190, 0, 0, new Vector3f(0,1,0), new Vector3f(0,cos190f,sin190f)));
		tests.add(new Test("Test38", 190, 0, 0, new Vector3f(0,0,1), new Vector3f(0,-sin190f,cos190f)));
		tests.add(new Test("Test39", 190, 0, 0, new Vector3f(1,1,0), new Vector3f(1,cos190f,sin190f)));
		tests.add(new Test("Test40", 0, 190, 0, new Vector3f(1,0,0), new Vector3f(cos190f,0,-sin190f)));
		tests.add(new Test("Test41", 0, 190, 0, new Vector3f(0,1,0), new Vector3f(0,1,0)));
		tests.add(new Test("Test42", 0, 190, 0, new Vector3f(0,0,1), new Vector3f(sin190f,0,cos190f)));
		tests.add(new Test("Test43", 0, 190, 0, new Vector3f(0,1,1), new Vector3f(sin190f,1,cos190f)));
		{
			int angleX = 261;
			int angleY = 38;
			double angleXd = Math.toRadians(angleX);
			double angleYd = Math.toRadians(angleY);
			double Cx = Math.cos(angleXd);
			double Sx = Math.sin(angleXd);
			double Cy = Math.cos(angleYd);
			double Sy = Math.sin(angleYd);
			Vector3f expected = new Vector3f(
					Cx+Sy,
					Sx*Sy+Cx-Sx*Cy,
					Cx*-Sy+Sx+Cx*Cy);
    		tests.add(new Test("Test44", angleX, angleY, 0, new Vector3f(1,1,1), expected));
		}
//		tests.add(new Test("Test13", 45, 0, 45, new Vector3f(0,1,0), new Vector3f(0,1,0)));
//		tests.add(new Test("Test13", 45, 0, 45, new Vector3f(0,0,1), new Vector3f(0,0,1)));
//		tests.add(new Test("Test13", 45, 0, 45, new Vector3f(1,1,0), new Vector3f(1,1,0)));
//		tests.add(new Test("Test13", 0, 45, 45, new Vector3f(1,0,0), new Vector3f(1,0,0)));
//		tests.add(new Test("Test13", 0, 45, 45, new Vector3f(0,1,0), new Vector3f(0,1,0)));
//		tests.add(new Test("Test13", 0, 45, 45, new Vector3f(0,0,1), new Vector3f(0,0,1)));
//		tests.add(new Test("Test13", 0, 45, 45, new Vector3f(1,1,0), new Vector3f(1,1,0)));
//		tests.add(new Test("Test12", 340, 0, 0, new Vector3f(1,1,0), new Vector3f(1,1,0)));
//		tests.add(new Test("Test13", 45, 315, 45, new Vector3f(1,1,1), new Vector3f(1,1,1)));
//		tests.add(new Test("Test13", -45, 315, 45, new Vector3f(1,1,1), new Vector3f(1,1,1)));
//		tests.add(new Test("Test13", 0, 0, 340, new Vector3f(1,1,1), new Vector3f(1,1,1)));
//		tests.add(new Test("Test13", 0, 45, 315, new Vector3f(1,1,1), new Vector3f(1,1,1)));
//		tests.add(new Test("Test13", -45, 45, 315, new Vector3f(1,1,1), new Vector3f(1,1,1)));
//		tests.add(new Test("Test13", 170, 340, 10, new Vector3f(1,1,1), new Vector3f(1,1,1)));
		Matrix4f m = new Matrix4f();
		float diff = 0;
		for (Test test : tests) {
			diff += test.run();
		}
		Log.d(TAG, "Total diff=" + diff);
	}
	
//	float assertEquals(String what, Rotate r1, Rotate r2) {
//		float diff = 0;
//		diff += assertEquals(what + " AngleX", r1.getAngleXDegrees(), r2.getAngleXDegrees());
//		diff += assertEquals(what + " AngleY", r1.getAngleYDegrees(), r2.getAngleYDegrees());
//		diff += assertEquals(what + " AngleZ", r1.getAngleZDegrees(), r2.getAngleZDegrees());
//		return diff;
//	}
	
	final float ZERO_THRESHOLD = 0.0001f;
	
	float assertEquals(String what, float v1, float v2) {
		float diff = Math.abs(v1-v2);
		if (Math.abs(diff) > ZERO_THRESHOLD) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("ERROR: ");
			sbuf.append(what);
			sbuf.append("->");
			sbuf.append(v1);
			sbuf.append("!=");
			sbuf.append(v2);
			sbuf.append(", diff=");
			sbuf.append(diff);
			Log.e(TAG, sbuf.toString());
		}
		return diff;
	}
	
	float assertEquals(String what, Vector3f v1, Vector3f v2) {
		float diff = 0;
		if (!v1.equals(v2)){
			double dx = Math.abs(v1.getX()-v2.getX());
			double dy = Math.abs(v1.getY()-v2.getY());
			double dz = Math.abs(v1.getZ()-v2.getZ());
			diff += dx + dy + dz;
    		if (dx > ZERO_THRESHOLD ||
    			dy > ZERO_THRESHOLD ||
    			dz > ZERO_THRESHOLD) {
    			StringBuffer sbuf = new StringBuffer();
    			sbuf.append("ERROR: ");
    			sbuf.append(what);
    			sbuf.append("->");
    			sbuf.append(v1.toString());
    			sbuf.append("!=");
    			sbuf.append(v2.toString());
    			sbuf.append(", diff=");
    			sbuf.append(diff);
    			Log.e(TAG, sbuf.toString());
    		}
		}
		return diff;
	}
    
}