package com.dugsolutions.nerdypig.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
		btn.setOnClickListener(v -> showStatsActivity());
		btn = (Button) findViewById(R.id.pig_fight);
		btn.setOnClickListener(v -> showBattleActivity());
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
		else if (id == R.id.action_test)
		{
			showTestActivity();
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
		Intent intent = new Intent(this, BattlePrepActivity.class);
		startActivity(intent);
	}

	void showRulesActivity()
	{
		Intent intent = new Intent(this, RulesActivity.class);
		startActivity(intent);
	}

	void showTestActivity()
	{
		Intent intent = new Intent(this, TestRollDice2.class);
		startActivity(intent);
	}

}
