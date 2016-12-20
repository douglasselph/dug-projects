package com.dugsolutions.nerdypig.act;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.dugsolutions.nerdypig.MyApplication;
import com.dugsolutions.nerdypig.R;

public class MainActivity extends AppCompatActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Button btn = (Button) findViewById(R.id.pig_nerd);
		btn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
                showStatsActivity();
			}
		});
		btn = (Button) findViewById(R.id.pig_fight);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showBattleActivity();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// noinspection SimplifiableIfStatement
		if (id == R.id.action_rules)
		{
			showRulesActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void showStatsActivity()
	{
		MyApplication app = (MyApplication) getApplicationContext();
		app.showStatsActivity(this, StatsActivity.ACTION_STATS);
	}

	void showBattleActivity()
	{
		Intent intent = new Intent(this, BattleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	void showRulesActivity()
	{
		Intent intent = new Intent(this, RulesActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

}