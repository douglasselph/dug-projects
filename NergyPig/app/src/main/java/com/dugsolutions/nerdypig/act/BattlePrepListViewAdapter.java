package com.dugsolutions.nerdypig.act;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dugsolutions.nerdypig.act.PlayerFragment.OnListFragmentInteractionListener;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.game.StrategyHolder;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link StrategyHolder} and makes a call
 * to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class BattlePrepListViewAdapter extends RecyclerView.Adapter<BattlePrepListViewAdapter.ViewHolder>
{
	public class ViewHolder extends RecyclerView.ViewHolder
	{
		public final View		mView;
		public final TextView	mDesc;
		public StrategyHolder	mItem;

		public ViewHolder(View view)
		{
			super(view);
			mView = view;
			mDesc = (TextView) view.findViewById(R.id.desc);
		}

		@Override
		public String toString()
		{
			return super.toString() + " '" + mDesc.getText() + "'";
		}

		void refreshText()
		{
			mDesc.setText(mItem.getDesc(mView.getContext()));
		}
	}

	private final List<StrategyHolder>				mValues;
	private final OnListFragmentInteractionListener	mListener;

	public BattlePrepListViewAdapter(List<StrategyHolder> items, OnListFragmentInteractionListener listener)
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
		holder.refreshText();

		holder.mView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				holder.mItem.setSelected();

				if (null != mListener)
				{
					mListener.onListFragmentInteraction(holder.mItem);
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
