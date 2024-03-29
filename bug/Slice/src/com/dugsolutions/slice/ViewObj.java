package com.dugsolutions.slice;

import java.io.File;
import java.io.FileOutputStream;

import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.dugsolutions.jacket.event.EventTapAdjust;
import com.dugsolutions.jacket.event.EventTapTwirl;
import com.dugsolutions.jacket.event.EventTapTwirl.Rotate;
import com.dugsolutions.jacket.event.IEventTap;
import com.dugsolutions.jacket.file.FileUtils;
import com.dugsolutions.jacket.math.Bounds3D;
import com.dugsolutions.jacket.math.Color4f;
import com.dugsolutions.jacket.math.Vector3f;
import com.dugsolutions.jacket.misc.PixelBuffer;
import com.dugsolutions.jacket.misc.Timing;
import com.dugsolutions.jacket.shape.Animator;
import com.dugsolutions.jacket.shape.Animator.OnPlayListener;
import com.dugsolutions.jacket.shape.Box;
import com.dugsolutions.jacket.shape.Shape;
import com.dugsolutions.jacket.view.ButtonGroup;
import com.dugsolutions.jacket.view.ButtonGroup.OnClickChangedListener;
import com.dugsolutions.jacket.view.ControlSurfaceView;
import com.dugsolutions.jacket.view.SpinnerControl;
import com.dugsolutions.slice.MyRenderer.PickShape;

public class ViewObj extends Activity
{

	interface CreateShape
	{
		Shape create();
	}

	class AdjustBones implements EventTapAdjust.Adjust
	{
		Vector3f	mStart;

		public void start(int x, int y)
		{

			PickShape pick = mRenderer.pick();
			Shape shape = pick.getShapeAt(x, y);

			// MatrixTrackingGL gl = mRenderer.getLastGL();

			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadIdentity();

			// Log.d("DEBUG", "TOUCH=" + x + ", " + y);

			// Vector3f pos = mCamera.getUnproject(gl, x, y);
			// Log.d("DEBUG", "POS-B=" + pos.toString());
			// mRoot.addChild(createPoint(pos, 0.1f, Color4f.BLACK));
			//
			// pos = mCamera.getWorldPosition(gl, x, y, 1);
			// Log.d("DEBUG", "POS-R=" + pos.toString());
			// mRoot.addChild(createPoint(pos, 0.2f, Color4f.RED));

			// pos = mCamera.getWorldPosition(gl, x, y, 2);
			// Log.d("DEBUG", "POS-G=" + pos.toString());
			// mRoot.addChild(createPoint(pos, 0.3f, Color4f.GREEN));

			// mCamera.test(gl);

			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// Matrix4f mv = gl.getMatrix();
			// Matrix4f mvi = new Matrix4f(mv).invert();
			// Log.d("DEBUG", "ModelView Matrix=" + mv.toString() + ", invert="
			// + mvi.toString());
			// Log.d("DEBUG", "Projection Matrix=" + mv.toString() + ", invert="
			// + mpi.toString());

			// Bounds mb = mActiveShape.getBounds();

			// Log.d("DEBUG", "Shape bounds=" + mb.toString());
			// Log.d("DEBUG", "Shape matrix=" +
			// mActiveShape.getMatrixMod().toString());

			// Log.d("DEBUG", "Point invert proj post=" +
			// mpi.applyPost(mStart).toString());
			// Log.d("DEBUG", "Point invert proj pre=" +
			// mpi.applyPre(mStart).toString());

			// ArrayList<Bone> list = mActiveShape.getBones((float)x, (float)y);
			// for (Bone bone : list) {
			// Log.d("DEBUG", "Matched " + bone.getName());
			// }
		}

		public void move(int x, int y)
		{
			// Vector3f vec = mCamera.getWorldPosition(x, y);
		}
	};

	class Controls
	{
		TableLayout	mHolder;
		TableRow	mControlRow;
		TableRow	mDisplayRow;
		ButtonGroup	mControlGroup;
		Spinner		mEglChoice;
		Spinner		mBlendChoice;
		Button		mPlay;

		Controls()
		{
			mHolder = new TableLayout(ViewObj.this);
			mHolder.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			mHolder.setStretchAllColumns(true);
			mHolder.setShrinkAllColumns(true);
			mHolder.setOrientation(TableLayout.HORIZONTAL);
			mHolder.setId(2);

			final Spinner shapeChoice = new Spinner(ViewObj.this);
			ArrayAdapter<SpinnerControl> adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this,
					android.R.layout.simple_spinner_item, new SpinnerControl[] {
							new SpinnerControl("Pyramid", DataManager.DATA_PYRAMID),
							new SpinnerControl("Cube", DataManager.DATA_CUBE),
							new SpinnerControl("Box", DataManager.DATA_BOX),
							new SpinnerControl("Susan", DataManager.DATA_SUSAN),
							new SpinnerControl("Hank", DataManager.DATA_HANK),
							new SpinnerControl("Wing1", DataManager.DATA_WING1),
							new SpinnerControl("WingArm", DataManager.DATA_WINGARM), });
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			shapeChoice.setAdapter(adapter);
			shapeChoice.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
				{
					SpinnerControl item = (SpinnerControl) shapeChoice.getSelectedItem();
					setShape(item.getArg());
				}

				public void onNothingSelected(AdapterView<?> parent)
				{
				}
			});
			shapeChoice
					.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			shapeChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getActiveShapeIndex()));

			mBlendChoice = new Spinner(ViewObj.this);
			adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this, android.R.layout.simple_spinner_item,
					new SpinnerControl[] {
							new SpinnerControl("Replace", GL10.GL_REPLACE),
							new SpinnerControl("Modulate", GL10.GL_MODULATE),
							new SpinnerControl("Decal", GL10.GL_DECAL), new SpinnerControl("Blend", GL10.GL_BLEND), });
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mBlendChoice.setAdapter(adapter);
			mBlendChoice.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
				{
					SpinnerControl item = (SpinnerControl) mBlendChoice.getSelectedItem();
					setBlendTexture(item.getArg());
				}

				public void onNothingSelected(AdapterView<?> parent)
				{
				}
			});
			mBlendChoice
					.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			mBlendChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getBlenderControl()));

			mEglChoice = new Spinner(ViewObj.this);
			adapter = new ArrayAdapter<SpinnerControl>(ViewObj.this, android.R.layout.simple_spinner_item,
					new SpinnerControl[] {
							new SpinnerControl("No EGL", EGL_NONE), new SpinnerControl("EGL Depth", EGL_DEPTH),
							new SpinnerControl("EGL NoDep", EGL_NO_DEPTH), });
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mEglChoice.setAdapter(adapter);
			mEglChoice.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
				{
					SpinnerControl item = (SpinnerControl) mEglChoice.getSelectedItem();
					setEGLDepth(item.getArg());
				}

				public void onNothingSelected(AdapterView<?> parent)
				{
				}
			});
			mEglChoice.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			mEglChoice.setSelection(SpinnerControl.locateSelection(adapter, mApp.getEGLDepth()));

			mPlay = new Button(ViewObj.this);
			mPlay.setText("Play");
			mPlay.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					playOrStop();

					runOnUiThread(new Runnable()
					{
						public void run()
						{
							if (mAnimator.isPlaying())
							{
								mPlay.setText("Stop");
							}
							else
							{
								mPlay.setText("Play");
							}
						}
					});
				}
			});

			ButtonGroup controlGroup = new ButtonGroup(ViewObj.this);
			controlGroup
					.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			mControlGroup = controlGroup;

			Button btnObj = new Button(ViewObj.this);
			btnObj.setId(CONTROL_OBJECT);
			btnObj.setText("Obj");
			btnObj.setLayoutParams(getParams());
			controlGroup.addView(btnObj);

			Button btnCam = new Button(ViewObj.this);
			btnCam.setId(CONTROL_CAMERA);
			btnCam.setText("Cam");
			btnCam.setLayoutParams(getParams());
			controlGroup.addView(btnCam);

			Button btnArm = new Button(ViewObj.this);
			btnArm.setId(CONTROL_ARMATURE);
			btnArm.setText("Arm");
			btnArm.setLayoutParams(getParams());
			controlGroup.addView(btnArm);

			controlGroup.setChecked(mApp.getActiveControl());
			controlGroup.setOnClickChangedListener(new OnClickChangedListener()
			{
				public void onClickChanged(View v)
				{
					mApp.setActiveControl(v.getId());
					setEventTap();

					if (v.getId() == CONTROL_ARMATURE)
					{
						showPlay();
					}
					else
					{
						hidePlay();
					}
				}
			});

			mControlRow = new TableRow(ViewObj.this);
			mControlRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			mControlRow.addView(shapeChoice);
			mControlRow.addView(controlGroup);

			mDisplayRow = new TableRow(ViewObj.this);
			mDisplayRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			mDisplayRow.addView(mEglChoice);
			mDisplayRow.addView(mBlendChoice);

			mHolder.addView(mDisplayRow);
			mHolder.addView(mControlRow);
		}

		LinearLayout.LayoutParams getParams()
		{
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.weight = 1;
			return params;
		}

		void showPlay()
		{
			mDisplayRow.removeAllViews();
			mDisplayRow.addView(mPlay);
			mDisplayRow.addView(mBlendChoice);
		}

		void hidePlay()
		{
			mDisplayRow.removeAllViews();
			mDisplayRow.addView(mEglChoice);
			mDisplayRow.addView(mBlendChoice);
		}

	};

	static final int			CONTROL_OBJECT		= 0;
	static final int			CONTROL_CAMERA		= 1;
	static final int			CONTROL_ARMATURE	= 2;

	static final int			EGL_NONE			= 0;
	static final int			EGL_DEPTH			= 1;
	static final int			EGL_NO_DEPTH		= 2;

	static final int			MENU_QUIT			= 0;
	static final int			MENU_RESET			= 1;
	static final int			MENU_SNAPSHOT		= 2;
	static final int			MENU_IMAGE			= 3;
	static final int			MENU_OUTLINE		= 4;

	public static final boolean	LOG					= true;
	public static final String	TAG					= "Slice";

	Shape						mRoot				= new Shape();
	Shape						mActiveShape		= null;
	EventTapTwirl				mTwirlEventTap;
	EventTapAdjust				mAdjustEventTap;
	IEventTap					mActiveEventTap;
	MyRenderer					mRenderer;
	ControlSurfaceView			mSurfaceView;
	MyApplication				mApp;
	Controls					mControls;
	Animator					mAnimator;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mApp = (MyApplication) getApplicationContext();

		RelativeLayout main = new RelativeLayout(this);
		main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// LinearLayout main = new LinearLayout(this);
		// main.setOrientation(LinearLayout.VERTICAL);

		mSurfaceView = new ControlSurfaceView(this);
		mSurfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSurfaceView.setId(1);

		// We want an 8888 pixel format because that's required for
		// a translucent window.
		// And we want a depth buffer.
		// mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

		// Use a surface format with an Alpha channel:
		// mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		mRenderer = new MyRenderer(mSurfaceView, mApp.getTM(), null, false);
		mRenderer.setBackground(new Color4f(0.5f, 1.0f, 1.0f));

		mApp.getDataManager().init(mApp.getTM());

		if (mApp.getEGLDepth() != EGL_NONE)
		{
			if (mApp.getEGLDepth() == EGL_DEPTH)
			{
				mSurfaceView.setEGLConfigChooser(true);
			}
			else
			{
				mSurfaceView.setEGLConfigChooser(false);
			}
		}
		mSurfaceView.setRenderer(mRenderer);

		mTwirlEventTap = new EventTapTwirl(mSurfaceView, new Rotate()
		{
			public void rotate(double xAngle, double yAngle)
			{
				mActiveShape.getMatrixMod().addRotate(xAngle, yAngle, 0.0);
			}
		});
		mAdjustEventTap = new EventTapAdjust(new AdjustBones());

		Runnable doubleTap = new Runnable()
		{
			public void run()
			{
				setNextEventTap();
			}
		};
		mTwirlEventTap.setDoubleTap(doubleTap);
		// mCamera.setDoubleTap(doubleTap);
		// mAdjustEventTap.setDoubleTap(doubleTap);

		mControls = new Controls();

		RelativeLayout.LayoutParams params;

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		main.addView(mControls.mHolder, params);

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ABOVE, mControls.mHolder.getId());
		main.addView(mSurfaceView, params);

		setContentView(main);
		mRenderer.setShape(mRoot);
		setShape(mApp.getDataManager().getShape(mApp.getActiveShapeIndex()));

		// TestData testData = new TestData();
		// testData.run();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		int order = 0;
		menu.add(0, MENU_QUIT, order++, "Quit");
		menu.add(0, MENU_RESET, order++, "Reset");
		menu.add(0, MENU_SNAPSHOT, order++, "Snapshot");
		menu.add(0, MENU_IMAGE, order++, "Image");
		menu.add(0, MENU_OUTLINE, order++, "Outline");
		return true;
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_QUIT:
				finish();
				break;
			case MENU_RESET:
				mActiveShape.resetRotate();
				mSurfaceView.requestRender();
				break;
			case MENU_SNAPSHOT:
			{
				try
				{
					final File file = FileUtils.GetExternalFile("screen.png", true);
					mSurfaceView.snapshot(file);
					Toast.makeText(ViewObj.this, "Created " + file.getAbsoluteFile(), Toast.LENGTH_SHORT).show();
				}
				catch (Exception ex)
				{
					Log.e(TAG, ex.getMessage());
				}
				break;
			}
			case MENU_IMAGE:
			{
				PixelBuffer pixelImage = new PixelBuffer((int) mRenderer.getWidth(), (int) mRenderer.getHeight());
				pixelImage.setRenderer(mRenderer);
				Bitmap bitmap = pixelImage.fill().flip().getBitmap();

				try
				{
					final File file = FileUtils.GetExternalFile("image.png", true);

					FileOutputStream fos = new FileOutputStream(file);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
					fos.flush();
					fos.close();

					Toast.makeText(ViewObj.this, "Created " + file.getAbsoluteFile(), Toast.LENGTH_SHORT).show();

				}
				catch (Exception ex)
				{
					Toast.makeText(ViewObj.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
				}
				break;
			}
			case MENU_OUTLINE:
			{
				mRenderer.setBackground(new Color4f(0.5f, 1.0f, 1.0f));

				if (mActiveShape.hasOutlineOverride())
				{
					mActiveShape.clearOutlineOverride();
				}
				else
				{
					mActiveShape.setOutlineOverride();
				}
				mSurfaceView.requestRender();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	void reload()
	{
		Intent intent = getIntent();
		overridePendingTransition(0, 0);
		finish();

		mApp.getDataManager().init(mApp.getTM());

		overridePendingTransition(0, 0);
		startActivity(intent);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mSurfaceView.onPause();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// int eglDepthPos = 0;
		// String msg;
		// if (mApp.getEGLDepth() == null) {
		// msg = "Use EGL with Depth";
		// } else if (!mApp.getEGLDepth()) {
		// msg = "Use EGL without Depth";
		// } else {
		// msg = "Disable EGL";
		// }
		// menu.getItem(eglDepthPos).setTitle(msg);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSurfaceView.onResume();
	}

	void setBlendTexture(int param)
	{
		if (mApp.getBlenderControl() != param)
		{
			mApp.setBlenderControl(param);
			mApp.getTM().setBlendParam(param);
			mSurfaceView.requestRender();
		}
	}

	void setEGLDepth(int param)
	{
		if (mApp.getEGLDepth() != param)
		{
			mApp.setEGLDepth(param);
			reload();
		}
	}

	// void setCullFace(int face) {
	// if (mApp.getCullFace().ordinal() != face) {
	// mApp.setCullFace(Shape.GetCullFaceFromOrdinal(face));
	// mActiveShape.setCullFace(mApp.getCullFace());
	// mSurfaceView.requestRender();
	// }
	// }

	// float assertEquals(String what, Rotate r1, Rotate r2) {
	// float diff = 0;
	// diff += assertEquals(what + " AngleX", r1.getAngleXDegrees(),
	// r2.getAngleXDegrees());
	// diff += assertEquals(what + " AngleY", r1.getAngleYDegrees(),
	// r2.getAngleYDegrees());
	// diff += assertEquals(what + " AngleZ", r1.getAngleZDegrees(),
	// r2.getAngleZDegrees());
	// return diff;
	// }

	public void setShape(int arg)
	{
		if (mApp.getActiveShapeIndex() != arg)
		{
			mApp.setActiveShapeIndex(arg);
			setShape(mApp.getDataManager().getShape(arg));
		}
	}

	void setShape(Shape shape)
	{
		Timing.Get(this).start("resetChildren");
		mRoot.resetChildren(shape);
		Timing.Get(this).end("resetChildren");

		mActiveShape = shape;

		mCamera.setLookAt(mActiveShape.getMidPoint());
		mCamera.setLocation(mCamera.getLookAt().dup());

		Bounds3D bounds = mActiveShape.getBounds();
		float offsetZ = bounds.getSizeZ();
		if (bounds.getSizeY() > bounds.getSizeX())
		{
			offsetZ += bounds.getSizeY();
		}
		else
		{
			offsetZ += bounds.getSizeX();
		}
		mCamera.getLocation().add(0, 0, offsetZ);

		mSurfaceView.setEventTap(mActiveEventTap);
		mSurfaceView.requestRender();
	}

	void setEventTap()
	{
		switch (mApp.getActiveControl())
		{
			case CONTROL_CAMERA:
				mActiveEventTap = mCamera;
				break;
			case CONTROL_OBJECT:
				mActiveEventTap = mTwirlEventTap;
				break;
			case CONTROL_ARMATURE:
			{
				StringBuffer sbuf = new StringBuffer();
				sbuf.append("#Bones=");
				sbuf.append(mActiveShape.getNumBones());
				sbuf.append(", #Joints=");
				sbuf.append(mActiveShape.getNumJoints());
				Toast.makeText(this, sbuf.toString(), Toast.LENGTH_LONG).show();
				mActiveEventTap = mAdjustEventTap;
				break;
			}
		}
		mSurfaceView.setEventTap(mActiveEventTap);
	}

	void setNextEventTap()
	{
		switch (mApp.getActiveControl())
		{
			case CONTROL_CAMERA:
				mApp.setActiveControl(CONTROL_OBJECT);
				break;
			case CONTROL_OBJECT:
				mApp.setActiveControl(CONTROL_ARMATURE);
				break;
			case CONTROL_ARMATURE:
				mApp.setActiveControl(CONTROL_CAMERA);
				break;
		}
		mControls.mControlGroup.setChecked(mApp.getActiveControl());
		setEventTap();
	}

	Shape createPoint(Vector3f loc, float size, Color4f color)
	{
		Shape point = new Box(size);
		point.setLocation(loc);
		point.setColor(color);
		return point;
	}

	void playOrStop()
	{
		if (mActiveShape.hasAnimation())
		{
			if (mAnimator != null && mAnimator.isPlaying())
			{
				mAnimator.stop();
				runOnUiThread(new Runnable()
				{
					public void run()
					{
						mControls.mPlay.setText("Play");
					}
				});
			}
			else
			{
				mAnimator = new Animator(mActiveShape);
				mAnimator.rewind();
				mAnimator.play(new OnPlayListener()
				{
					public void onFrame(Animator animator, int frame)
					{
						mSurfaceView.requestRender();
					}

					public void onFinished()
					{
						runOnUiThread(new Runnable()
						{
							public void run()
							{
								mControls.mPlay.setText("Play");
								mAnimator.rewind();
								mSurfaceView.requestRender();
							}
						});
					}
				});
			}
		}
	}

}