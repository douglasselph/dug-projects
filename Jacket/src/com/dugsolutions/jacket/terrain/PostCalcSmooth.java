package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;

import android.util.Log;

import com.dugsolutions.jacket.math.Bounds2D;

public class PostCalcSmooth
{
	public enum Direction
	{
		Down, Up, Right, Left;
	}

	ArrayList<PointInfo>	mAdjustPos	= new ArrayList<PointInfo>();
	float					mAverageDelta;
	Bounds2D				mBounds;
	float					mCurX;
	float					mExtremeDelta;
	int						mExtremeIPos;
	IMapData				mMap;
	Direction				mDirection;
	FloatMap<PointInfo>		mPointMap;

	/**
	 * 
	 * @param direction
	 * @param bounds
	 */
	public PostCalcSmooth(Direction direction, Bounds2D bounds)
	{
		mBounds = bounds;
		mDirection = direction;
	}

	void adjust(int ipos, float amt)
	{
		PointInfo info = mAdjustPos.get(ipos);
		float zval = info.getVec().getZ() + amt;
		info.getVec().setZ(zval);
		mMap.putZ(info);
	}

	public void run(IMapData map)
	{
		mMap = map;
		/*
		 * Get the first point just within the boundary.
		 */
		int[] boundary = map.getBoundary(mBounds);
		if (boundary == null)
		{
			return;
		}
		int startRow = boundary[0];
		int startCol = boundary[1];
		int endRow = boundary[2];
		int endCol = boundary[3];
		float delta;
		float lastZ;
		float z;
		float maxDelta;
		float decline;
		float amt;
		int maxPos;
		int p;
		int dist;

		if (mDirection == Direction.Down)
		{
			for (int col = startCol; col <= endCol; col++)
			{
				Log.d("DEBUG", "COLUMN " + col);
				/*
				 * Gather column of entries
				 */
				PointInfo info;
				mAdjustPos.clear();
				for (int row = startRow; row <= endRow; row++)
				{
					mAdjustPos.add(info = mMap.getPointInfo(row, col));
					Log.d("DEBUG", "ROW " + row + ":" + info.getVec().getZ());
				}
				/*
				 * Locate most extreme delta point.
				 */
				lastZ = mAdjustPos.get(0).getVec().getZ();
				maxDelta = 0;
				maxPos = 0;

				for (p = 1; p < mAdjustPos.size(); p++)
				{
					z = mAdjustPos.get(p).getVec().getZ();
					delta = z - lastZ;
					if (Math.abs(delta) > Math.abs(maxDelta))
					{
						maxDelta = delta;
						maxPos = p;
					}
				}
				Log.d("DEBUG", "MOST EXTREME=" + (startRow + maxPos) + ", " + maxDelta);
				// /*
				// * All bets are off if we are already at the end.
				// */
				// if (maxPos + 1 < mAdjustPos.size())
				// {
				// dist = mAdjustPos.size() - maxPos + 1;
				// decline = maxDelta / dist;
				// amt = -maxDelta;
				// for (p = maxPos; p < mAdjustPos.size(); p++)
				// {
				// adjust(p, amt);
				// amt += decline;
				// }
				// }
			}
		}
	}
}
