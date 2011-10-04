package com.tipsolutions.jacket.shape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Timer;
import java.util.TimerTask;

import com.tipsolutions.jacket.math.Matrix4f;
import com.tipsolutions.jacket.math.Point;
import com.tipsolutions.jacket.math.Quaternion;
import com.tipsolutions.jacket.math.Vector3f;
import com.tipsolutions.jacket.math.Vector4f;
import com.tipsolutions.jacket.shape.Shape.AnimSet;
import com.tipsolutions.jacket.shape.Shape.AnimType;
import com.tipsolutions.jacket.shape.Shape.Bone;
import com.tipsolutions.jacket.shape.Shape.AnimSet.AnimControl;

public class Animator {

	class CMatrix {
		Matrix4f mMatrix;
		float mTime = -1;
		
		Matrix4f evaluate(float atTime) {
			mTime = atTime;
			
			Vector3f loc = new Vector3f();
			Vector4f quat = new Vector4f();
			Vector4f rot = new Vector4f();
			Vector3f scale = new Vector3f();
			
			boolean setLoc = false;
			boolean setQuat = false;
			boolean setRot = false;
			boolean setScale = false;
			
			for (AnimType type : mAnimSet.getControlKeys()) {
				AnimControl anim = mAnimSet.getAnim(type);
				Point [] pairs = anim.getPairs(atTime);
				
				if (pairs != null) {
					float value;
					
					if (pairs.length == 1) {
						value = pairs[0].getValue();
					} else {
						Compute compute = getCompute(pairs[0], pairs[1]);
						value = compute.evaluate(atTime);
					}
					if (type == AnimType.LOC_X) {
						setLoc = true;
						loc.setX(value);
					} else if (type == AnimType.LOC_Y) {
						setLoc = true;
						loc.setY(value);
					} else if (type == AnimType.LOC_Z) {
						setLoc = true;
						loc.setZ(value);
					} else if (type == AnimType.ROT_X) {
						setRot = true;
						rot.setX(value);
					} else if (type == AnimType.ROT_Y) {
						setRot = true;
						rot.setY(value);
					} else if (type == AnimType.ROT_Z) {
						setRot = true;
						rot.setZ(value);
					} else if (type == AnimType.QUAT_X) {
						setQuat = true;
						quat.setX(value);
					} else if (type == AnimType.QUAT_Y) {
						setQuat = true;
						quat.setY(value);
					} else if (type == AnimType.QUAT_Z) {
						setQuat = true;
						quat.setZ(value);
					} else if (type == AnimType.QUAT_W) {
						setQuat = true;
						quat.setW(value);
					} else if (type == AnimType.SCALE_X) {
						setScale = true;
						scale.setX(value);
					} else if (type == AnimType.SCALE_Y) {
						setScale = true;
						scale.setY(value);
					} else if (type == AnimType.SCALE_Z) {
						setScale = true;
						scale.setZ(value);
					}
				}
			}
			if (setQuat) {
				Quaternion q = new Quaternion(quat.getX(),quat.getY(),quat.getZ(),quat.getW());
				mMatrix = q.toRotationMatrix4f();
			} else {
    			mMatrix = new Matrix4f();
			}
			if (setRot) {
				mMatrix.setRotate(rot.getX(), rot.getY(), rot.getZ());
			}
			if (setLoc) {
				mMatrix.setLocation(loc);
			}
			if (setScale) {
				Matrix4f m = new Matrix4f();
				m.setScale(new Vector4f(scale));
				mMatrix.mult(m);
			}
			return mMatrix;
		}
	}
	
	abstract class Compute {
		protected final Point mPt1;
		protected final Point mPt2;
		
		public Compute(Point pt1, Point pt2) {
			mPt1 = pt1;
			mPt2 = pt2;
		}
		
		public abstract float evaluate(float time);
	}
	
	class ComputeBezier extends Compute {
		protected Point mPtMid;
		protected float mDeltaTime;
		
		public ComputeBezier(Point pt1, Point pt2) {
			super(pt1, pt2);
			
			mDeltaTime = pt2.getTime() - pt1.getTime();
			mPtMid = new Point();
			mPtMid.setTime(pt2.getTime() - (pt2.getTime() - pt1.getTime())/4);
			mPtMid.setValue(mPt2.getValue());
		}

		@Override
		public float evaluate(float time) {
			float deltaTime = time - mPt1.getTime();
			float deltaTimeRatio = deltaTime / mDeltaTime;
			float invT = 1 - deltaTimeRatio;
			
			float value = (invT*invT * mPt1.getValue()) + 
					      (2*invT*deltaTimeRatio * mPtMid.getValue()) + 
					      (deltaTimeRatio*deltaTimeRatio * mPt2.getValue());
			return value;
		}
	}
	
	class ComputeLinear extends Compute {
		protected float mSlope;
		
		public ComputeLinear(Point pt1, Point pt2) {
			super(pt1, pt2);
			float deltaTime = mPt2.getTime() - mPt1.getTime();
			float deltaValue = mPt2.getValue() - mPt1.getValue();
			mSlope = deltaValue / deltaTime;
		}

		@Override
		public float evaluate(float time) {
			float deltaTime = time - mPt1.getTime();
			float deltaValue = mSlope * deltaTime;
			return deltaValue + mPt1.getValue();
		}
	}
	
	public enum Interpolation {
		Bezier,
		Linear
	};
	
	static public interface OnPlayListener {
		void onFrame(Animator animator, int frame);
		void onFinished();
	};
	
	protected final Shape mShape;
	protected final Bone mBone;
	protected final AnimSet mAnimSet;
	protected float mInterval = 0.1f; // seconds
	protected float mAtTime = 0;
	protected Interpolation mInterpolation = Interpolation.Linear;;
	protected FloatBuffer mVertexOrig = null;
	protected Timer mTimer = null;
	protected int mFrame;
	protected boolean mIsPlaying = false;

	public Animator(Shape shape, Bone bone, String animSetName) {
		mShape = shape;
		mBone = bone;
		mAnimSet = bone.getAnimSet(animSetName);
	}
	
	public Animator(Shape shape) {
		mShape = shape;
		mBone = mShape.getAnimBone();
		mAnimSet = mBone.getAnimSet(0);
	}
	
	protected Compute getCompute(Point pt1, Point pt2) {
		if (mInterpolation == Interpolation.Linear) {
			return new ComputeLinear(pt1, pt2);
		} else if (mInterpolation == Interpolation.Bezier) {
			return new ComputeBezier(pt1, pt2);
		}
		return null;
	}
	
	public float getCurTime() { return mAtTime; }
	public boolean isPlaying() { return mIsPlaying; }
	public Shape getShape() { return mShape; }
	public Bone getBone() { return mBone; }
	
	void makeCopy() {
		if (mVertexOrig != null) {
			return;
		}
		FloatBuffer vertexBuf = mShape.getVertexBuf();
		
		mBone.rewind();
		vertexBuf.rewind();
		
		ByteBuffer buf = ByteBuffer.allocateDirect(3*4*mBone.limit());
		buf.order(ByteOrder.nativeOrder()); // Get this from android platform
		mVertexOrig = buf.asFloatBuffer();
		
		while (mBone.hasRemaining()) {
			vertexBuf.position(mBone.get()*3);
			mVertexOrig.put(vertexBuf.get());
			mVertexOrig.put(vertexBuf.get());
			mVertexOrig.put(vertexBuf.get());
		}
	}
	
	public boolean next() {
		if (mAtTime < 0) {
			rewind();
		} else if (mAtTime < mAnimSet.getEndTime()) {
    		toTime(mAtTime + mInterval);
		} else {
			return false;
		}
		return true;
	}
	
	public void play(final OnPlayListener playListener) {
		if (mTimer != null) {
			mTimer.cancel();
		}
		mTimer = new Timer();
		mFrame = 0;
		mIsPlaying = true;
		
		long milliInterval = (long) (mInterval * 1000);
		
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (next()) {
    				playListener.onFrame(Animator.this, ++mFrame);
				} else {
    				playListener.onFinished();
					mTimer.cancel();
					mIsPlaying = false;
				}
			}
		}, milliInterval, milliInterval);
	}
	
	public void stop() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}
	
	public void rewind() {
		if (mVertexOrig != null) {
    		FloatBuffer vertexBuf = mShape.getVertexBuf();
    		vertexBuf.rewind();
    		mBone.rewind();
    		mVertexOrig.rewind();
    		
    		while (mVertexOrig.hasRemaining()) {
    			vertexBuf.position(mBone.get()*3);
    			vertexBuf.put(mVertexOrig.get());
    			vertexBuf.put(mVertexOrig.get());
    			vertexBuf.put(mVertexOrig.get());
    		}
		} else {
			makeCopy();
		}
		mAtTime = mAnimSet.getStartTime();
	}
	
	public void setInpolation(Interpolation interpolation) {
		mInterpolation = interpolation;
	}
	
	public void setInterval(float interval) {
		mInterval = interval;
	}
	
	public void toTime(float time) {
		if (time > mAnimSet.getEndTime()) {
			time = mAnimSet.getEndTime();
		}
		CMatrix cmatrix = new CMatrix();
		Matrix4f m = cmatrix.evaluate(time);
		
		makeCopy();
		
		FloatBuffer vertexBuf = mShape.getVertexBuf();
	
		vertexBuf.rewind();
		mBone.rewind();
		mVertexOrig.rewind();
		
		Vector4f value;
		
		while (mVertexOrig.hasRemaining()) {
			value = new Vector4f(mVertexOrig.get(), 
								 mVertexOrig.get(), 
								 mVertexOrig.get(), 1);
			m.apply(value);
			vertexBuf.position(mBone.get()*3);
			vertexBuf.put(value.getX());
			vertexBuf.put(value.getY());
			vertexBuf.put(value.getZ());
		}
		mAtTime = time;
	}
	
}
