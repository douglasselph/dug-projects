package com.tipsolutions.slice;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemSelectedListener;

import com.tipsolutions.jacket.data.Box;
import com.tipsolutions.jacket.data.Pyramid;
import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.data.ShapeGL;
import com.tipsolutions.jacket.data.Shape.dFloatBuf;
import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.image.TextureManager.Texture;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.Matrix3f;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.view.ControlCamera;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.SpinnerControl;
import com.tipsolutions.jacket.view.TwirlEventTap;
import com.tipsolutions.jacket.view.TwirlEventTap.Rotate;

public class Main extends Activity {

	interface CreateShape {
		ShapeGL create();
	}
	
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
	
	static final int EGL_NONE = 0;
	static final int EGL_DEPTH = 1;
	static final int EGL_NO_DEPTH = 2;
	
	static final int MENU_QUIT = 0;
	static final int MENU_RESET = 1;
	
    Data [] mData;
    
    public static final boolean LOG = true;
    public static final String TAG = "Slice";
    
    final String CUBE_FILE = "cube.data";
    final String SUSAN_FILE = "suzanne.data";
    final String HANK_FILE = "hank.data";
    final String WING1_FILE = "wingL.data";
    static final int NUM_FILES = 4;
    
    static final int ARRAY_SIZE = NUM_FILES+2;
    ShapeGL [] mShapes = new ShapeGL[ARRAY_SIZE];
    ShapeGL mActiveShape = null;
    TwirlEventTap mTwirlEventTap;
    ControlCamera mCamera;
    MyRenderer mRenderer;
    ControlSurfaceView mSurfaceView;
    MyApplication mApp;

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
    
    View createControls() {
        TableLayout holder = new TableLayout(this);
        holder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        holder.setStretchAllColumns(true);
        holder.setShrinkAllColumns(true);
        holder.setOrientation(TableLayout.HORIZONTAL);
        holder.setId(2);
        
        final Spinner shapeChoice = new Spinner(this);
        ArrayAdapter<SpinnerControl> adapter = new ArrayAdapter<SpinnerControl>(this,
        		android.R.layout.simple_spinner_item,
        		new SpinnerControl[] {
        		  new SpinnerControl("Pyramid", DATA_PYRAMID),
        		  new SpinnerControl("Cube", DATA_CUBE),
        		  new SpinnerControl("Box", DATA_BOX),
        		  new SpinnerControl("Susan", DATA_SUSAN),
        		  new SpinnerControl("Hank", DATA_HANK),
        		  new SpinnerControl("Wing1", DATA_WING1),
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shapeChoice.setAdapter(adapter);
        shapeChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				SpinnerControl item = (SpinnerControl) shapeChoice.getSelectedItem();
				setShape(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        });
        shapeChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        shapeChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getActiveShapeIndex()));
        
        final Spinner blendChoice = new Spinner(this);
        adapter = new ArrayAdapter<SpinnerControl>(this,
        		android.R.layout.simple_spinner_item,
        		new SpinnerControl[] {
        		  new SpinnerControl("Replace", GL10.GL_REPLACE),
        		  new SpinnerControl("Modulate", GL10.GL_MODULATE),
        		  new SpinnerControl("Decal", GL10.GL_DECAL),
        		  new SpinnerControl("Blend", GL10.GL_BLEND),
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        blendChoice.setAdapter(adapter);
        blendChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				SpinnerControl item = (SpinnerControl) blendChoice.getSelectedItem();
				setBlendTexture(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        	
        });
        blendChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        blendChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getBlenderControl()));
        
        final Spinner eglChoice = new Spinner(this);
        adapter = new ArrayAdapter<SpinnerControl>(this,
        		android.R.layout.simple_spinner_item,
        		new SpinnerControl[] {
        		  new SpinnerControl("No EGL", EGL_NONE),
        		  new SpinnerControl("EGL Depth", EGL_DEPTH),
        		  new SpinnerControl("EGL NoDep", EGL_NO_DEPTH),
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eglChoice.setAdapter(adapter);
        eglChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				SpinnerControl item = (SpinnerControl) eglChoice.getSelectedItem();
				setEGLDepth(item.getArg());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        });
        eglChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        eglChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getEGLDepth()));
        
        Button btn = new Button(this);
        btn.setText("Set Pos");
        btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
        });
        
//        final Spinner cullChoice = new Spinner(this);
//        adapter = new ArrayAdapter<SpinnerControl>(this,
//        		android.R.layout.simple_spinner_item,
//        		new SpinnerControl[] {
//        		  new SpinnerControl("Cull None", Shape.CullFace.NONE.ordinal()),
//        		  new SpinnerControl("Cull Back", Shape.CullFace.BACK.ordinal()),
//        		  new SpinnerControl("Cull Front", Shape.CullFace.FRONT.ordinal()),
//                });
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        cullChoice.setAdapter(adapter);
//        cullChoice.setOnItemSelectedListener(new OnItemSelectedListener() {
//			@Override
//			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//				SpinnerControl item = (SpinnerControl) cullChoice.getSelectedItem();
//				setCullFace(item.arg);
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> parent) {
//			}
//        });
//        cullChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
//        cullChoice.setSelection(locateSelection(adapter, mApp.getCullFace().ordinal()));

        TableRow tableRow;
        tableRow = new TableRow(this);
        tableRow.addView(eglChoice);
//        tableRow.addView(cullChoice);
        holder.addView(tableRow);
        
        tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        tableRow.addView(shapeChoice);
        tableRow.addView(blendChoice);
        holder.addView(tableRow);
        
        return holder;
    }
   
    ShapeGL getBox() {
//        TextureManager.Texture texture = mRenderer.getTextureManager().getTexture(R.raw.robot);
        TextureManager.Texture texture = mRenderer.getTextureManager().getTexture("feather_real.png");
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
        TextureManager.Texture texture = mRenderer.getTextureManager().getTexture(R.raw.robot);
        ShapeGL shape = new Pyramid(1f, 1f, texture);
        shape.setLocation(new Vector3f(0f, -shape.getBounds().getSizeY()/2, 0));
        setColors(shape);
        return shape;
    }
    
    ShapeGL loadShape(String file) {
        ShapeGL shape = new ShapeGL();
        try {
            InputStream inputStream = getAssets().open(file);
            shape.readData(inputStream, mRenderer.getTextureManager());
        } catch (Exception ex) {
        	Log.e(MyApplication.TAG, ex.getMessage());
        }
        return shape;
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mApp = (MyApplication) getApplicationContext();
        
        mData = new Data[ARRAY_SIZE];
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
        RelativeLayout main = new RelativeLayout(this);
        main.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
//        LinearLayout main = new LinearLayout(this);
//        main.setOrientation(LinearLayout.VERTICAL);
        
        mCamera = new ControlCamera();
    
        mSurfaceView = new ControlSurfaceView(this);
        mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mSurfaceView.setId(1);
      
        // We want an 8888 pixel format because that's required for
        // a translucent window.
        // And we want a depth buffer.
//        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        
        // Use a surface format with an Alpha channel:
//        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        
        mRenderer = new MyRenderer(mSurfaceView, null, mCamera, false);
        mRenderer.setClippingPlaneColor(new Color4f(0.5f, 1.0f, 1.0f));
        
        if (mApp.getEGLDepth() != EGL_NONE) {
        	if (mApp.getEGLDepth() == EGL_DEPTH) {
                mSurfaceView.setEGLConfigChooser(true);
        	} else {
                mSurfaceView.setEGLConfigChooser(false);
        	}
        }
        mSurfaceView.setRenderer(mRenderer);
        
        mTwirlEventTap = new TwirlEventTap(mSurfaceView, new Rotate() {
			@Override
			public void rotate(double xAngle, double yAngle) {
				mActiveShape.getMatrixMod().addRotate(xAngle, yAngle, 0.0);
			}
        });
        mTwirlEventTap.setDoubleTap(new Runnable() {
			@Override
			public void run() {
				mSurfaceView.setEventTap(mCamera);
			}
        });
        mCamera.setDoubleTap(new Runnable() {
			@Override
			public void run() {
				mSurfaceView.setEventTap(mTwirlEventTap);
			}
        });
        View controls = createControls();
      
        RelativeLayout.LayoutParams params;
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        
        main.addView(controls, params);
        
        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ABOVE, controls.getId());
        main.addView(mSurfaceView, params);
    
        setContentView(main);
		setShape(mData[mApp.getActiveShapeIndex()].getShape());
        
        testMatrixAddRotate();
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int order = 0;
		menu.add(0, MENU_QUIT, order++, "Quit");
		menu.add(0, MENU_RESET, order++, "Reset");
		return true;
	};
   
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case MENU_QUIT:
    			finish();
    			break;
    		case MENU_RESET:
				mActiveShape.resetRotate();
				mSurfaceView.requestRender();
				break;
    	}
		return super.onOptionsItemSelected(item);
	}
    
    void reload() {
    	Intent intent = getIntent();
    	overridePendingTransition(0, 0);
    	finish();

    	overridePendingTransition(0, 0);
    	startActivity(intent);
    }

	@Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
//		int eglDepthPos = 0;
//		String msg;
//		if (mApp.getEGLDepth() == null) {
//			msg = "Use EGL with Depth";
//		} else if (!mApp.getEGLDepth()) {
//			msg = "Use EGL without Depth";
//		} else {
//			msg = "Disable EGL";
//		}
//		menu.getItem(eglDepthPos).setTitle(msg);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }
	
	void setBlendTexture(int param) {
		if (mApp.getBlenderControl() != param) {
        	TextureManager tm = mRenderer.getTextureManager();
        	mApp.setBlenderControl(param);
        	tm.setBlendParam(param);
            mSurfaceView.requestRender();
		}
    }
	
	void setEGLDepth(int param) {
		if (mApp.getEGLDepth() != param) {
    		mApp.setEGLDepth(param);
    		reload();	
		}
	}
	
//	void setCullFace(int face) {
//		if (mApp.getCullFace().ordinal() != face) {
//			mApp.setCullFace(Shape.GetCullFaceFromOrdinal(face));
//			mActiveShape.setCullFace(mApp.getCullFace());
//            mSurfaceView.requestRender();
//		}
//	}
	
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
	
//	float assertEquals(String what, Rotate r1, Rotate r2) {
//		float diff = 0;
//		diff += assertEquals(what + " AngleX", r1.getAngleXDegrees(), r2.getAngleXDegrees());
//		diff += assertEquals(what + " AngleY", r1.getAngleYDegrees(), r2.getAngleYDegrees());
//		diff += assertEquals(what + " AngleZ", r1.getAngleZDegrees(), r2.getAngleZDegrees());
//		return diff;
//	}
	
	public void setShape(int arg) {
		if (mApp.getActiveShapeIndex() != arg) {
    		mApp.setActiveShapeIndex(arg);
    		setShape(mData[arg].getShape());
		}
	}
	
	void setShape(ShapeGL shape) {
    	mActiveShape = shape;
        mRenderer.setShape(shape);
        
    	mCamera.setLookAt(mActiveShape.getMidPoint());
    	mCamera.setLocation(mCamera.getLookAt().dup());
    	
    	Shape.Bounds bounds = mActiveShape.getBounds();
    	float offsetZ = bounds.getSizeZ();
    	if (bounds.getSizeY() > bounds.getSizeX()) {
    		offsetZ += bounds.getSizeY();
    	} else {
    		offsetZ += bounds.getSizeX();
    	}
    	mCamera.getLocation().add(0, 0, offsetZ);
        
        mSurfaceView.setEventTap(mCamera);
        mSurfaceView.requestRender();
    }
	
	void testMatrixAddRotate() {
		
		class Test {
			String mName;
			Vector3f mVecInit;
			float mRotX, mRotY, mRotZ;
			
			Test(String name, int rx, int ry, int rz, Vector3f initVec) {
				mName = name;
				mRotX = (float) Math.toRadians(rx);
				mRotY = (float) Math.toRadians(ry);
				mRotZ = (float) Math.toRadians(rz);
				mVecInit = initVec;
			}
			
			Vector3f getExpected() {
				Matrix3f m = new Matrix3f();
				m.setRotate(mRotX, mRotY, mRotZ);
				return m.apply(mVecInit,null);
			}
			
			float run() {
				float diff = 0;
				Quaternion quat = new Quaternion();
				quat.fromAngles(mRotX, mRotY, mRotZ);
				
				Vector3f vec = new Vector3f(quat.apply(mVecInit, null));
				diff += assertEquals(mName, vec, getExpected());
				
				return diff;
			}
		};
		ArrayList<Test> tests = new ArrayList<Test>();
		tests.add(new Test("Test01", 0, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test02", 45, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test03", 45, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test04", 45, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test05", 0, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test06", 0, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test07", 0, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test08", 0, 0, 45, new Vector3f(1,0,0)));
		tests.add(new Test("Test09", 0, 0, 45, new Vector3f(0,1,0)));
		tests.add(new Test("Test10", 0, 0, 45, new Vector3f(0,0,1)));
		tests.add(new Test("Test11", -45, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Test12", 0, -45, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Test13", 0, 0, -45, new Vector3f(1,1,0)));
		tests.add(new Test("Test14", 315, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Test15", 0, 315, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Test16", 0, 0, 315, new Vector3f(1,1,0)));
		tests.add(new Test("Test17", 45, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test18", 45, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test19", 45, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test20", -45, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test21", -45, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test22", -45, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test13", 315, 45, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test24", 315, 45, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test25", 315, 45, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test26", 45, 0, -45, new Vector3f(1,0,0)));
		tests.add(new Test("Test27", 45, 0, -45, new Vector3f(0,1,0)));
		tests.add(new Test("Test28", 45, 0, -45, new Vector3f(0,0,1)));
		tests.add(new Test("Test29", 45, 0, -45, new Vector3f(0,1,1)));
		tests.add(new Test("Test30", 45, 0, -45, new Vector3f(1,0,1)));
		tests.add(new Test("Test31", 45, 0, -45, new Vector3f(1,1,0)));
		tests.add(new Test("Test32", 45, 0, -45, new Vector3f(1,1,1)));
		tests.add(new Test("Test33", 45, 0, 315, new Vector3f(1,0,0)));
		tests.add(new Test("Test34", 45, 0, 315, new Vector3f(0,1,0)));
		tests.add(new Test("Test35", 45, 0, 315, new Vector3f(0,0,1)));
		tests.add(new Test("Test36", 190, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test37", 190, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test38", 190, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test39", 190, 0, 0, new Vector3f(1,1,0)));
		tests.add(new Test("Test40", 0, 190, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test41", 0, 190, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test42", 0, 190, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test43", 0, 190, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Test44", 0, 0, 190, new Vector3f(1,0,0)));
		tests.add(new Test("Test45", 0, 0, 190, new Vector3f(0,1,0)));
		tests.add(new Test("Test46", 0, 0, 190, new Vector3f(0,0,1)));
		tests.add(new Test("Test47", 0, 0, 190, new Vector3f(0,1,1)));
		tests.add(new Test("Test48", 300, 0, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test49", 300, 0, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test50", 300, 0, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test51", 300, 0, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Test52", 0, 300, 0, new Vector3f(1,0,0)));
		tests.add(new Test("Test53", 0, 300, 0, new Vector3f(0,1,0)));
		tests.add(new Test("Test54", 0, 300, 0, new Vector3f(0,0,1)));
		tests.add(new Test("Test55", 0, 300, 0, new Vector3f(0,1,1)));
		tests.add(new Test("Test56", 0, 0, 300, new Vector3f(1,0,0)));
		tests.add(new Test("Test57", 0, 0, 300, new Vector3f(0,1,0)));
		tests.add(new Test("Test58", 0, 0, 300, new Vector3f(0,0,1)));
		tests.add(new Test("Test59", 0, 0, 300, new Vector3f(0,1,1)));
		tests.add(new Test("Test60", 261, 38, 0, new Vector3f(1,1,1)));
		float diff = 0;
		for (Test test : tests) {
			diff += test.run();
		}
		Log.d(TAG, "Total diff=" + diff);
	}
	
}