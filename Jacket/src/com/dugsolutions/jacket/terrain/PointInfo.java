package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;

import com.dugsolutions.jacket.math.Vector3f;

public class PointInfo
{
	Vector3f			mNormal;
	Vector3f			mVec;
	ArrayList<Integer>	mPositions;

	public PointInfo(Vector3f vec, Vector3f normal)
	{
		mNormal = normal;
		mVec = vec;
	}

	public Vector3f getNormal()
	{
		return mNormal;
	}

	public Vector3f getVec()
	{
		return mVec;
	}

	public void setNormal(Vector3f normal)
	{
		mNormal = normal;
	}

	public void setVec(Vector3f vec)
	{
		mVec = vec;
	}

	public void addPosition(int pos)
	{
		if (mPositions == null)
		{
			mPositions = new ArrayList<Integer>();
		}
		mPositions.add(pos);
	}

	public ArrayList<Integer> getPositions()
	{
		return mPositions;
	}
}
