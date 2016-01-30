package com.dugsolutions.jacket.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Bounds3D
{

	protected static final int	MIN_X	= 0;
	protected static final int	MIN_Y	= 1;
	protected static final int	MIN_Z	= 2;
	protected static final int	MAX_X	= 3;
	protected static final int	MAX_Y	= 4;
	protected static final int	MAX_Z	= 5;

	protected static final int	SIZ		= 6;

	float[]						mBounds	= new float[SIZ];

	public float getMaxX()
	{
		return mBounds[MAX_X];
	}

	public float getMaxY()
	{
		return mBounds[MAX_Y];
	}

	public float getMaxZ()
	{
		return mBounds[MAX_Z];
	}

	public float getMidX()
	{
		return (getMaxX() + getMinX()) / 2;
	}

	public float getMidY()
	{
		return (getMaxY() + getMinY()) / 2;
	}

	public float getMidZ()
	{
		return (getMaxZ() + getMinZ()) / 2;
	}

	public float getMinX()
	{
		return mBounds[MIN_X];
	}

	public float getMinY()
	{
		return mBounds[MIN_Y];
	}

	public float getMinZ()
	{
		return mBounds[MIN_Z];
	}

	public float getSizeX()
	{
		return getMaxX() - getMinX();
	}

	public float getSizeY()
	{
		return getMaxY() - getMinY();
	}

	public float getSizeZ()
	{
		return getMaxZ() - getMinZ();
	}

	public Bounds3D()
	{

	}

	public Bounds3D(ComputeBounds computeBounds)
	{
		set(computeBounds);
	}

	public void read(DataInputStream dataStream) throws IOException
	{
		setMinX(dataStream.readFloat());
		setMinY(dataStream.readFloat());
		setMinZ(dataStream.readFloat());
		setMaxX(dataStream.readFloat());
		setMaxY(dataStream.readFloat());
		setMaxZ(dataStream.readFloat());
	}

	public void set(ComputeBounds computeBounds)
	{
		mBounds = new float[SIZ];

		setMinX(computeBounds.minX);
		setMinY(computeBounds.minY);
		setMinZ(computeBounds.minZ);
		setMaxX(computeBounds.maxX);
		setMaxY(computeBounds.maxY);
		setMaxZ(computeBounds.maxZ);
	}

	public void setMaxX(float x)
	{
		mBounds[MAX_X] = x;
	}

	public void setMaxY(float y)
	{
		mBounds[MAX_Y] = y;
	}

	public void setMaxZ(float z)
	{
		mBounds[MAX_Z] = z;
	}

	public void setMinX(float x)
	{
		mBounds[MIN_X] = x;
	}

	public void setMinY(float y)
	{
		mBounds[MIN_Y] = y;
	}

	public void setMinZ(float z)
	{
		mBounds[MIN_Z] = z;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("Bounds[X ");
		sbuf.append(getMinX());
		sbuf.append(",");
		sbuf.append(getMaxX());
		sbuf.append(", Y ");
		sbuf.append(getMinY());
		sbuf.append(",");
		sbuf.append(getMaxY());
		sbuf.append(", Z ");
		sbuf.append(getMinZ());
		sbuf.append(",");
		sbuf.append(getMaxZ());
		sbuf.append("]");
		return sbuf.toString();
	}

	public boolean within(float x, float y)
	{
		return (x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY());
	}

	public boolean within(float x, float y, float z)
	{
		return (x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY() && z >= getMinZ() && z <= getMaxZ());
	}

	public void write(DataOutputStream dataStream) throws IOException
	{
		dataStream.writeFloat(getMinX());
		dataStream.writeFloat(getMinY());
		dataStream.writeFloat(getMinZ());
		dataStream.writeFloat(getMaxX());
		dataStream.writeFloat(getMaxY());
		dataStream.writeFloat(getMaxZ());
	}

	public Bounds2D getBounds2D()
	{
		return new Bounds2D(getMinX(), getMinY(), getMaxX(), getMaxY());
	}
}
