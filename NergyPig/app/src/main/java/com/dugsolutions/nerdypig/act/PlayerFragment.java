package com.dugsolutions.nerdypig.act;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.game.BattleLine;
import com.dugsolutions.nerdypig.game.StrategyHolder;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PlayerFragment extends Fragment
{
	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnListFragmentInteractionListener
	{
		void onListFragmentInteraction(StrategyHolder item);
	}

	static final String					ARG_COLUMN_COUNT	= "column-count";
	int									mColumnCount		= 1;
	OnListFragmentInteractionListener	mListener;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public PlayerFragment()
	{
	}

	@SuppressWarnings("unused")
	public static PlayerFragment newInstance(int columnCount)
	{
		PlayerFragment fragment = new PlayerFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_COLUMN_COUNT, columnCount);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getArguments() != null)
		{
			mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View topView = inflater.inflate(R.layout.fragment_battle_list, container, false);
		View view = topView.findViewById(R.id.list);

		// Set the adapter
		if (view instanceof RecyclerView)
		{
			Context context = view.getContext();
			RecyclerView recyclerView = (RecyclerView) view;
			if (mColumnCount <= 1)
			{
				recyclerView.setLayoutManager(new LinearLayoutManager(context));
			}
			else
			{
				recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
			}
			recyclerView.setAdapter(new BattlePrepListViewAdapter(BattleLine.getItems(), mListener));
		}
		return view;
	}

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);

		if (context instanceof OnListFragmentInteractionListener)
		{
			mListener = (OnListFragmentInteractionListener) context;
		}
		else
		{
			throw new RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		mListener = null;
	}

}
