package com.dugsolutions.jacket.terrain;

import java.util.ArrayList;

import com.dugsolutions.jacket.math.Bounds2D;

public class PostCalcSmooth
{
	public enum Orientation
	{
		Horizontal, Vertical;
	}

	ArrayList<PointInfo>	mAdjustPos	= new ArrayList<PointInfo>();
	float					mAverageDelta;
	Bounds2D				mBounds;
	float					mCurX;
	float					mExtremeDelta;
	int						mExtremeIPos;
	IMapData				mMap;
	Orientation				mOrientation;
	FloatMap<PointInfo>		mPointMap;

	public PostCalcSmooth(Orientation orientation, Bounds2D bounds)
	{
		mBounds = bounds;
		mOrientation = orientation;
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
		float totalDelta;
		float averageDelta;
		float adjustMax;
		float adjustInc;
		float amt;
		int maxPos;

		if (mOrientation == Orientation.Horizontal)
		{
			for (int col = startCol; col <= endCol; col++)
			{
				/*
				 * Gather column of entries
				 */
				mAdjustPos.clear();
				for (int row = startRow; row <= endRow; row++)
				{
					mAdjustPos.add(mMap.getPointInfo(row, col));
				}
				/*
				 * Find max delta point & average delta
				 */
				lastZ = mAdjustPos.get(0).getVec().getZ();
				maxDelta = 0;
				totalDelta = 0;
				maxPos = 0;

				for (int p = 1; p < mAdjustPos.size(); p++)
				{
					z = mAdjustPos.get(p).getVec().getZ();
					delta = z - lastZ;
					if (Math.abs(delta) > Math.abs(maxDelta))
					{
						maxDelta = delta;
						maxPos = p;
					}
					totalDelta += delta;
				}
				averageDelta = totalDelta / mAdjustPos.size();
				/*
				 * Compute principle adjustment
				 */
				adjustMax = (maxDelta - averageDelta) / 2;

				if (maxPos > 0)
				{
					/*
					 * Compute how much the principle adjustment can be reduced each time such that the value will be
					 * 0.5
					 * over half the distance.
					 */

					adjustInc = (float) Math.pow(0.5, 1 / (maxPos / 2.0));
					/*
					 * Apply the adjustment to all values BEFORE the max delta.
					 */
					amt = adjustMax;
					for (int p = maxPos - 1; p >= 0; p--)
					{
						adjust(p, amt);
						amt *= adjustInc;
					}
				}
				int left = mAdjustPos.size() - maxPos;
				if (left > 0)
				{
					/*
					 * Compute how much the principle adjustment can be reduced each time such that the value will be
					 * 0.5
					 * over half the distance.
					 */
					adjustInc = (float) Math.pow(0.5, 1 / (left / 2.0));
					/*
					 * Apply the adjustment to all values AFTER the max delta.
					 */
					amt = -adjustMax;
					for (int p = maxPos; p < mAdjustPos.size(); p++)
					{
						adjust(p, amt);
						amt *= adjustInc;
					}
				}
			}
		}
	}
}
