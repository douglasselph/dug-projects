package com.tipsolutions.bugplug.map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;
import android.view.MotionEvent;

import com.tipsolutions.jacket.image.TextureManager;
import com.tipsolutions.jacket.math.Bounds2D;
import com.tipsolutions.jacket.math.Color4f;
import com.tipsolutions.jacket.math.MaterialColors;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.Vector4f;
import com.tipsolutions.jacket.view.ControlRenderer;
import com.tipsolutions.jacket.view.ControlSurfaceView;
import com.tipsolutions.jacket.view.EventTapAdjust;
import com.tipsolutions.jacket.view.EventTapAdjust.Adjust;

public class RenderMap extends ControlRenderer implements Adjust
{
	static final boolean	mHasSpotLight	= false;
	static final float		GLOBAL_AMBIENT	= 0.25f;
	static final float		GLOBAL_DIFFUSE	= 0.5f;
	static final float		GLOBAL_SPECULAR	= 0.82f;
	static final float		INITIAL_TILT	= -15f;

	Map						mMap;
	EventTapAdjust			mEventTap;
	float					mMaxZ;
	Bounds2D				mMaxBounds;
	Vector3f				mRotateAngle;
	Vector4f				mSpotPos;
	Vector4f				mGlobalPos;
	MaterialColors			mGlobalColor	= new MaterialColors();
	MaterialColors			mSpotColor		= new MaterialColors();
	boolean					mIsPan			= true;
	boolean					mUpdateLights;

	public RenderMap(ControlSurfaceView view, TextureManager tm)
	{
		super(view, tm);

		mMap = new Map(tm);
		mEventTap = new EventTapAdjust(this);
		mRotateAngle = new Vector3f(INITIAL_TILT, 0, 0);
	}

	void applyRotate(GL10 gl)
	{
		if (mRotateAngle.getX() != 0)
		{
			gl.glRotatef(mRotateAngle.getX(), 1, 0, 0);
		}
		if (mRotateAngle.getY() != 0)
		{
			gl.glRotatef(mRotateAngle.getY(), 0, 1, 0);
		}
		if (mRotateAngle.getZ() != 0)
		{
			gl.glRotatef(mRotateAngle.getZ(), 0, 0, 1);
		}
	}

	public Bounds2D getBounds()
	{
		return mMaxBounds;
	}

	public MaterialColors getGlobalMatColors()
	{
		return mGlobalColor;
	}

	public Vector4f getGlobalPos()
	{
		return mGlobalPos;
	}

	public MaterialColors getGroundMatColors()
	{
		return mMap.getGroundMatColors();
	}

	public MaterialColors getSpotMatColors()
	{
		return mSpotColor;
	}

	public Vector4f getSpotPos()
	{
		return mSpotPos;
	}

	public MaterialColors getWaterMatColors()
	{
		return mMap.getWaterMatColors();
	}

	public boolean isPan()
	{
		return mIsPan;
	}

	@Override
	public void onDrawFrameContents(GL10 gl)
	{
		super.onDrawFrameContents(gl);

		if (mUpdateLights)
		{
			setLights(gl);
			mUpdateLights = false;
		}
		mCamera.applyViewBounds(gl);
		applyRotate(gl);
		mMap.onDraw(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		super.onSurfaceChanged(gl, width, height);

		mCamera.setViewBounds(mMap.getBounds());
		mMaxZ = mCamera.getViewingLoc().getZ();
		mMaxBounds = new Bounds2D(mCamera.getViewBounds());
		mMaxBounds.setMinX(mMaxBounds.getMinX() * 1.4f);
		mMaxBounds.setMaxX(mMaxBounds.getMaxX() * 1.4f);
		mSpotPos.set(mMaxBounds.getSizeX() / 2, 0, -1f, 1);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);

		initDepth(gl);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		mGlobalColor.setAmbient(new Color4f(GLOBAL_AMBIENT, GLOBAL_AMBIENT, GLOBAL_AMBIENT, 1));
		mGlobalColor.setDiffuse(new Color4f(GLOBAL_DIFFUSE, GLOBAL_DIFFUSE, GLOBAL_DIFFUSE, 1f));
		mGlobalColor.setSpecular(new Color4f(GLOBAL_SPECULAR, GLOBAL_SPECULAR, GLOBAL_SPECULAR, 1f));
		mGlobalPos = new Vector4f(0f, 0f, 1f, 0);

		mSpotColor.setAmbient(new Color4f(Color4f.BLACK));
		mSpotColor.setDiffuse(new Color4f(Color4f.BLACK));
		mSpotColor.setSpecular(new Color4f(Color4f.WHITE));
		mSpotPos = new Vector4f(0, 0, -1, 1);

		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);

		if (mHasSpotLight)
		{
			gl.glEnable(GL10.GL_LIGHT1);
		}
		setLights(gl);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		return mEventTap.onTouchEvent(ev);
	}

	public void pan(float xDelta, float yDelta)
	{
		if (mIsPan)
		{
			mCamera.pan(xDelta, yDelta, mMaxBounds);
		}
		else
		{
			float delta;
			if (Math.abs(xDelta) > Math.abs(yDelta))
			{
				delta = xDelta;
			}
			else
			{
				delta = yDelta;
			}
			float degrees = (float) (180 * -delta);
			mRotateAngle.setZ(mRotateAngle.getZ() + degrees);
		}
	}

	public void resetView()
	{
		mRotateAngle = new Vector3f();
		mIsPan = true;
		mCamera.setViewBounds(mMap.getBounds());
		mView.requestRender();
	}

	@Override
	public void scale(float delta)
	{
		mCamera.scale(delta, mMaxZ);
	}

	public void setIsPan(boolean flag)
	{
		mIsPan = flag;
	}

	void setLights(GL10 gl)
	{
		/* AMBIENT LIGHT */
		gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, mGlobalColor.getAmbient().toArray(), 0);
		/* GENERAL LIGHT */
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, mGlobalColor.getDiffuse().toArray(), 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, mGlobalColor.getSpecular().toArray(), 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, mGlobalPos.toArray(), 0);

		if (mHasSpotLight)
		{
			/* SPECULAR HIGHLIGHT */
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, mSpotPos.toArray(), 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, mSpotColor.getAmbient().toArray(), 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, mSpotColor.getDiffuse().toArray(), 0);
			gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPECULAR, mSpotColor.getSpecular().toArray(), 0);
		}
	}

	/**
	 * Indicate the tilt.
	 * 
	 * @param unit
	 *        : translates to 15 degrees per value.
	 */
	public void setTilt(float unit)
	{
		float degrees = unit * -15;
		mRotateAngle.setX(degrees);

		if (unit == 0)
		{
			mRotateAngle.setZ(0);
		}
		mView.requestRender();
	}

	public int getTilt()
	{
		return (int) FloatMath.floor(mRotateAngle.getX() / -15f);
	}

	@Override
	public String toString()
	{
		return mMap.toString();
	}

	public void updateGlobalLights()
	{
		mUpdateLights = true;
	}
}
