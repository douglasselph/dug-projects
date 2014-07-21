package com.tipsolutions.panda;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.tipsolutions.panda.data.CellMap;
import com.tipsolutions.panda.data.Strokes;

public class StrokesView extends View
{
	class Grid
	{
		int	mStartX;
		int	mStartY;
		int	mNumCellsX;
		int	mNumCellsY;
		int	mCellWidth;
		int	mCellHeight;

		Grid(int cellWidth, int cellHeight, int numx, int numy, int offsetx, int offsety)
		{
			mCellWidth = cellWidth;
			mCellHeight = cellHeight;
			mNumCellsX = numx;
			mNumCellsY = numy;
			mStartX = offsetx;
			mStartY = offsety;
		}

		void drawCells(Canvas canvas)
		{
			int cellx;
			int celly;
			Rect r = new Rect();

			r.top = mStartY;
			r.bottom = r.top + mCellHeight;

			for (celly = 0; celly < mNumCellsY; celly++)
			{
				r.left = mStartX;
				r.right = r.left + mCellWidth;

				for (cellx = 0; cellx < mNumCellsX; cellx++)
				{
					if (isOn(cellx, celly))
					{
						canvas.drawRect(r, mBoxInteriorPaint);
					}
					r.left += mCellWidth;
					r.right += mCellWidth;
				}
				r.top += mCellHeight;
				r.bottom += mCellHeight;
			}
		}

		void drawGrid(Canvas canvas)
		{
			int cellx;
			int celly;
			float startX;
			float startY;
			float lineY;
			float lineX;
			float stopX;
			float stopY;

			startX = mStartX;
			stopX = mStartX + mNumCellsX * mCellWidth;
			lineY = mStartY;

			for (celly = 0; celly <= mNumCellsY; celly++)
			{
				canvas.drawLine(startX, lineY, stopX, lineY, mBoxEdgePaint);
				lineY += mCellHeight;
			}
			startY = mStartY;
			stopY = mStartY + mNumCellsY * mCellHeight;
			lineX = mStartX;
			for (cellx = 0; cellx <= mNumCellsX; cellx++)
			{
				canvas.drawLine(lineX, startY, lineX, stopY, mBoxEdgePaint);
				lineX += mCellWidth;
			}
		}
	};

	MyApplication	mApp;
	Grid			mGrid;
	Strokes			mStrokes;
	CellMap			mMap;
	Paint			mBoxInteriorPaint;
	Paint			mBoxEdgePaint;
	int				mLevel	= 0;

	public StrokesView(Context context)
	{
		super(context);
		init(null);
	}

	public StrokesView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public StrokesView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	@Override
	protected int getSuggestedMinimumHeight()
	{
		return 100;
	}

	@Override
	protected int getSuggestedMinimumWidth()
	{
		return 100;
	}

	void init(AttributeSet attrs)
	{
		mApp = (MyApplication) getContext().getApplicationContext();

		if (attrs != null)
		{
			TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.StrokesView, 0, 0);

			try
			{
				mLevel = a.getInt(R.styleable.StrokesView_level, 0);
			}
			finally
			{
				a.recycle();
			}
		}
		mBoxInteriorPaint = new Paint();
		int color = getResources().getColor(R.color.box_fill);
		mBoxInteriorPaint.setStyle(Paint.Style.FILL);
		mBoxInteriorPaint.setColor(color);
		color = getResources().getColor(R.color.box_edge);
		mBoxEdgePaint = new Paint();
		mBoxEdgePaint.setColor(color);
		// mBoxSize = getResources().getDimensionPixelSize(R.dimen.box_size);
		// mMinNumBoxesOnEdge = getResources().getInteger(R.integer.min_num_boxes_on_edge);
	}

	void install(int w, int h) throws Exception
	{
		mMap = mStrokes.getCellMap();

		int numCellsFigX = mStrokes.getBounds().width();
		int numCellsFigY = mStrokes.getBounds().height();
		int padCellsLeft = mStrokes.getBounds().left;
		int padCellsTop = mStrokes.getBounds().top;
		int padCellsRight = padCellsLeft;
		int padCellsBottom = padCellsTop;
		int numCellsX = numCellsFigX + padCellsLeft + padCellsRight;
		int numCellsY = numCellsFigY + padCellsTop + padCellsBottom;
		int cellSizeX = w / numCellsX;
		int cellSizeY = h / numCellsY;
		int cellSize = (cellSizeX > cellSizeY ? cellSizeY : cellSizeX);
		int offsetx = padCellsLeft * cellSize;
		int offsety = padCellsTop * cellSize;

		mGrid = new Grid(cellSize, cellSize, numCellsFigX, numCellsFigY, offsetx, offsety);
	}

	boolean isOn(int x, int y)
	{
		return mMap.is(x, y);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		mGrid.drawCells(canvas);
		mGrid.drawGrid(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
		int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));
		int minh = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();
		int h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh);

		setMeasuredDimension(w, h);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);

		try
		{
			install(w, h);
		}
		catch (Exception ex)
		{
			Log.e(MyApplication.TAG, ex.getMessage());
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return super.onTouchEvent(event);
	}

	public void setStrokes(Strokes strokes) throws Exception
	{
		mStrokes = strokes;

		install(getWidth(), getHeight());
		invalidate();
	}
}
