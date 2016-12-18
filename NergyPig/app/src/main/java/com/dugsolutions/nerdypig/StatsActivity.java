package com.dugsolutions.nerdypig;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.dugsolutions.nerdypig.R;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        TextView textView = (TextView) findViewById(R.id.stats);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    /**
     * This is called when the Home (Up) button is pressed in the Action Bar.
     */
    public static boolean navigateUp(Activity act, Class claz)
    {
        Intent parentActivityIntent = new Intent(act, claz);
        parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(parentActivityIntent);
        act.finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home)
        {
            return navigateUp(this, MainActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }


}
