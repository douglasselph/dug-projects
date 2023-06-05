package com.dugsolutions.nerdypig.act;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.dugsolutions.nerdypig.R;

import java.util.Random;

public class TestRollDice2 extends AppCompatActivity
{
	/**
	 * Handler to manage the rolling of the die.
	 *
	 * @author Jason Cavett
	 */
	class DiceRollHandler extends Handler
	{
		/**
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		public void handleMessage(Message msg)
		{
			int value = mRand.nextInt(6);
			ImageView dice = (ImageView) TestRollDice2.this.findViewById(R.id.dice);
			dice.setImageLevel(value);

			// If there are still rolls available, roll another time.
			Integer rollsLeft = (Integer) msg.obj;
			if (rollsLeft > 0)
			{
				TestRollDice2.this.mDiceHandler
						.sendMessageDelayed(Message.obtain(TestRollDice2.this.mDiceHandler, 0, --rollsLeft), 200);
			}
		}
	}

	Random			mRand			= new Random();
	DiceRollHandler	mDiceHandler	= new DiceRollHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_roll_dice2);
	}

	/**
	 * Handle the roll of the dice.
	 *
	 * @param view
	 *        The view that is being acted upon.
	 */
	public void rollDie(View view)
	{
		View dice = findViewById(R.id.dice);

		int dieWidth = dice.getWidth();

		View parent = (View) dice.getParent();
		int parentWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();

		float distToEdge = parentWidth - dice.getX() - dieWidth;
		float unitsToEdge = distToEdge / (float) dieWidth;

		final int initialTime = 500;
		final int endingTime = 650;
		final float verticalDist = (8 * mRand.nextFloat() - 4) / 10f;

		AnimationSet set = new AnimationSet(true);
		set.setFillAfter(true);

		TranslateAnimation tanim = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, unitsToEdge,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, verticalDist);
		tanim.setDuration(initialTime);
		set.addAnimation(tanim);
		TranslateAnimation tanim2 = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0f,
				TranslateAnimation.RELATIVE_TO_SELF, -unitsToEdge/2,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, verticalDist);
		tanim2.setStartOffset(initialTime);
		tanim2.setDuration(endingTime);
		tanim2.setInterpolator(new DecelerateInterpolator());
		set.addAnimation(tanim2);

 		dice.startAnimation(set);

		mDiceHandler.sendMessageDelayed(Message.obtain(mDiceHandler, 0, 7), 200);
	}
}
