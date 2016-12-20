package com.dugsolutions.nerdypig.act;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;
import com.dugsolutions.nerdypig.db.GameEnd;
import com.dugsolutions.nerdypig.db.GlobalInt;

public class RulesActivity extends AppCompatActivity
{
	EditText		mMaxTurns;
	EditText		mEndPoints;
	EditText		mNumGames;
	RadioButton		mRbMaxTurns;
	RadioButton		mRbEndPoints;
	Toolbar			mToolbar;
	MyApplication	mApp;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rules);
		mApp = (MyApplication) getApplicationContext();
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setTitle();
		setSupportActionBar(mToolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_email);
		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				mApp.doEmail(RulesActivity.this);
			}
		});

		mMaxTurns = (EditText) findViewById(R.id.edit_turns);
		mMaxTurns.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					int value = queryValue(v);
					if (value > 0)
					{
						GlobalInt.setMaxTurns(value);
					}
					return true;
				}
				return false;
			}
		});
		mEndPoints = (EditText) findViewById(R.id.edit_points);
		mEndPoints.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					int value = queryValue(v);
					if (value > 0)
					{
						GlobalInt.setEndPoints(value);
					}
					return true;
				}
				return false;
			}
		});
		mNumGames = (EditText) findViewById(R.id.edit_games);
		mNumGames.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					int value = queryValue(v);
					if (value > 0)
					{
						GlobalInt.setNumGames(value);
					}
					return true;
				}
				return false;
			}
		});
		mRbEndPoints = (RadioButton) findViewById(R.id.rb_points);
		mRbMaxTurns = (RadioButton) findViewById(R.id.rb_turns);

		mRbEndPoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					GlobalInt.setGameEnd(GameEnd.END_POINTS);
					mRbMaxTurns.setChecked(false);
				}
			}
		});
		mRbMaxTurns.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					GlobalInt.setGameEnd(GameEnd.MAX_TURNS);
					mRbEndPoints.setChecked(false);
				}
			}
		});
	}

	int queryValue(TextView tv)
	{
		try
		{
			int value = Integer.valueOf(tv.getText().toString());
			if (value > 0)
			{
				return value;
			}
			else
			{
				new AlertDialog.Builder(getContext()).setTitle(R.string.entry_error).setMessage(R.string.error_positive)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int whichButton)
							{
							}
						}).show();
			}
		}
		catch (Exception ex)
		{
			new AlertDialog.Builder(getContext()).setTitle(R.string.entry_error).setMessage(ex.getMessage())
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int whichButton)
						{
						}
					}).show();
		}
		return -1;
	}

	Context getContext()
	{
		return this;
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		mMaxTurns.setText(String.valueOf(GlobalInt.getMaxTurns()));
		mEndPoints.setText(String.valueOf(GlobalInt.getEndPoints()));
		mNumGames.setText(String.valueOf(GlobalInt.getNumGames()));

		if (GlobalInt.getGameEnd() == GameEnd.END_POINTS)
		{
			mRbEndPoints.setChecked(true);
			mRbMaxTurns.setChecked(false);
		}
		else
		{
			mRbEndPoints.setChecked(false);
			mRbMaxTurns.setChecked(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();

		if (itemId == android.R.id.home)
		{
			return MyApplication.navigateUp(this, MainActivity.class);
		}
		return super.onOptionsItemSelected(item);
	}

	void setTitle()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getString(R.string.title_activity_rules));
		sbuf.append(" - ");
		sbuf.append(mApp.getVersion());
		mToolbar.setTitle(sbuf.toString());
	}
}
