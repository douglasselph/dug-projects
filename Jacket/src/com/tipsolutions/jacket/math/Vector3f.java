package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;

public class Vector3f
{

	static public final Vector3f	UNIT_X	= new Vector3f(1, 0, 0);
	static public final Vector3f	UNIT_Y	= new Vector3f(0, 1, 0);
	static public final Vector3f	UNIT_Z	= new Vector3f(0, 0, 1);
	static public final Vector3f	ZERO	= new Vector3f(0, 0, 0);

	protected float[]				mData;

	public Vector3f()
	{
		mData = new float[3];
	}

	public Vector3f(double x, double y, double z)
	{
		mData = new float[3];
		mData[0] = (float) x;
		mData[1] = (float) y;
		mData[2] = (float) z;
	}

	public Vector3f(float x, float y, float z)
	{
		mData = new float[3];
		mData[0] = x;
		mData[1] = y;
		mData[2] = z;
	}

	public Vector3f(float[] v)
	{
		mData = new float[3];
		for (int i = 0; i < mData.length && i < v.length; i++)
		{
			mData[i] = v[i];
		}
	}

	public Vector3f(final Vector3 v)
	{
		mData = new float[3];
		mData[0] = (float) v.mX;
		mData[1] = (float) v.mY;
		mData[2] = (float) v.mZ;
	}

	public Vector3f(final Vector3f v)
	{
		mData = new float[3];
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			mData[i] = v.mData[i];
		}
	}

	public Vector3f add(float x, float y, float z)
	{
		mData[0] += x;
		mData[1] += y;
		mData[2] += z;
		return this;
	}

	public Vector3f add(final Vector3f v)
	{
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			mData[i] += v.mData[i];
		}
		return this;
	}

	public Vector3f addX(float x)
	{
		mData[0] += x;
		return this;
	}

	public Vector3f addY(float y)
	{
		mData[1] += y;
		return this;
	}

	public Vector3f addZ(float z)
	{
		mData[2] += z;
		return this;
	}

	public void apply(GL10 gl)
	{
		gl.glTranslatef(getX(), getY(), getZ());
	}

	/**
	 * Compute the cross product of the current vector u to the incoming vector
	 * v. Thus the return is "u x v" where "u" is the this vector.
	 * 
	 * @param v
	 *        : second operand of the cross product
	 * @return cross product
	 */
	public Vector3f cross(final Vector3f v)
	{
		float x = getX();
		float y = getY();
		float z = getZ();
		float x2 = v.getX();
		float y2 = v.getY();
		float z2 = v.getZ();
		mData[0] = (y * z2) - (z * y2); /* x */
		mData[1] = (z * x2) - (x * z2); /* y */
		mData[2] = (x * y2) - (y * x2); /* z */
		return this;
	}

	public Vector3f divide(float scalar)
	{
		mData[0] /= scalar;
		mData[1] /= scalar;
		mData[2] /= scalar;
		return this;
	}

	public Vector3f divide(float x, float y, float z)
	{
		mData[0] /= x;
		mData[1] /= y;
		mData[2] /= z;
		return this;
	}

	public Vector3f divide(final Vector3f v)
	{
		mData[0] /= v.getX();
		mData[1] /= v.getY();
		mData[2] /= v.getZ();
		return this;
	}

	public Vector3f dup()
	{
		return new Vector3f(this);
	}

	public boolean equals(final Vector3f v)
	{
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			if (mData[i] != v.mData[i])
			{
				return false;
			}
		}
		return true;
	}

	public float getX()
	{
		return mData[0];
	}

	public float getY()
	{
		return mData[1];
	}

	public float getZ()
	{
		return mData[2];
	}

	public float length()
	{
		return FloatMath.sqrt(lengthSquared());
	}

	public float lengthSquared()
	{
		return getX() * getX() + getY() * getY() + getZ() * getZ();
	}

	public Vector3f multiply(float scalar)
	{
		for (int i = 0; i < mData.length; i++)
		{
			mData[i] *= scalar;
		}
		return this;
	}

	public Vector3f multiply(float x, float y, float z)
	{
		mData[0] *= x;
		mData[1] *= y;
		mData[2] *= z;
		return this;
	}

	public Vector3f multiply(final Vector3f v)
	{
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			mData[i] *= v.mData[i];
		}
		return this;
	}

	public Vector3f negate()
	{
		return multiply(-1f);
	}

	public Vector3f normalize()
	{
		final double lengthSq = lengthSquared();
		if (Math.abs(lengthSq) > MathUtils.EPSILON)
		{
			return multiply((float) MathUtils.inverseSqrt(lengthSq));
		}
		return this;
	}

	public Vector3f put(FloatBuffer buf)
	{
		buf.put(getX()).put(getY()).put(getZ());
		return this;
	}

	public Vector3f set(double x, double y, double z)
	{
		mData[0] = (float) x;
		mData[1] = (float) y;
		mData[2] = (float) z;
		return this;
	}

	public Vector3f set(float x, float y, float z)
	{
		mData[0] = x;
		mData[1] = y;
		mData[2] = z;
		return this;
	}

	public Vector3f set(final Vector3 v)
	{
		mData[0] = (float) v.mX;
		mData[1] = (float) v.mY;
		mData[2] = (float) v.mZ;
		return this;
	}

	public Vector3f set(final Vector3f v)
	{
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			mData[i] = v.mData[i];
		}
		return this;
	}

	public void setX(double x)
	{
		mData[0] = (float) x;
	}

	public void setX(float x)
	{
		mData[0] = x;
	}

	public void setY(double y)
	{
		mData[1] = (float) y;
	}

	public void setY(float y)
	{
		mData[1] = y;
	}

	public void setZ(double z)
	{
		mData[2] = (float) z;
	}

	public void setZ(float z)
	{
		mData[2] = z;
	}

	public Vector3f subtract(float x, float y, float z)
	{
		mData[0] -= x;
		mData[1] -= y;
		mData[2] -= z;
		return this;
	}

	public Vector3f subtract(final Vector3f v)
	{
		for (int i = 0; i < mData.length && i < v.mData.length; i++)
		{
			mData[i] -= v.mData[i];
		}
		return this;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("(");
		sbuf.append(getX());
		sbuf.append(",");
		sbuf.append(getY());
		sbuf.append(",");
		sbuf.append(getZ());
		sbuf.append(")");
		return sbuf.toString();
	}

	/**
	 * 
	 * @param format
	 *        : specified format. For example, "%.3f"
	 * @return
	 */
	public String toString(String format)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("(");
		sbuf.append(String.format(format, getX()));
		sbuf.append(",");
		sbuf.append(String.format(format, getY()));
		sbuf.append(",");
		sbuf.append(String.format(format, getZ()));
		sbuf.append(")");
		return sbuf.toString();
	}

	public void zero()
	{
		for (int i = 0; i < mData.length; i++)
		{
			mData[i] = 0;
		}
	}
}
