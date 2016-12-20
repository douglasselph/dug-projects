package com.dugsolutions.nerdypig.act;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dugsolutions.nerdypig.act.PlayerFragment.OnListFragmentInteractionListener;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.battle.BattleStrategy;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link BattleStrategy} and makes a call
 * to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyPlayerRecyclerViewAdapter extends RecyclerView.Adapter<MyPlayerRecyclerViewAdapter.ViewHolder>
{
	public class ViewHolder extends RecyclerView.ViewHolder
	{
		public final View		mView;
		public final TextView	mIdView;
		public final TextView	mContentView;
		public BattleStrategy	mItem;

		public ViewHolder(View view)
		{
			super(view);
			mView = view;
			mIdView = (TextView) view.findViewById(R.id.id);
			mContentView = (TextView) view.findViewById(R.id.content);
		}

		@Override
		public String toString()
		{
			return super.toString() + " '" + mContentView.getText() + "'";
		}

		void refreshId()
		{
			mIdView.setText(mItem.getId());
		}

		void refreshText()
		{
			mContentView.setText(mItem.toString(mView.getContext()));
		}
	}

	private final List<BattleStrategy>				mValues;
	private final OnListFragmentInteractionListener	mListener;

	public MyPlayerRecyclerViewAdapter(List<BattleStrategy> items, OnListFragmentInteractionListener listener)
	{
		mValues = items;
		mListener = listener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_strategy_line, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, int position)
	{
		holder.mItem = mValues.get(position);
		holder.refreshId();
		holder.refreshText();

		holder.mView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				holder.mItem.incBattlePoints();
				holder.refreshId();

				if (null != mListener)
				{
					if (mListener.onListFragmentInteraction(holder.mItem))
					{
						notifyDataSetChanged();
					}
				}
			}
		});
	}

	@Override
	public int getItemCount()
	{
		return mValues.size();
	}

}
