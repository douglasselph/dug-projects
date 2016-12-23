package com.dugsolutions.nerdypig.act;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.dugsolutions.nerdypig.R;

import java.util.ArrayList;
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

//		AnimatorSet set = new AnimatorSet();
//		ArrayList<Animator> list = new ArrayList<>();
//		ObjectAnimator oa = ObjectAnimator.ofFloat(dice, "xFraction", 0f, 0.5f);
//		list.add(oa);
//		ObjectAnimator oa2 = ObjectAnimator.ofFloat(dice, "xFraction", 0.5f, 0.3f);
//		list.add(oa2);
//		set.playSequentially(list);
//		set.setDuration(2000);
//		set.setInterpolator(new DecelerateInterpolator());
//		set.start();

		AnimationSet set = new AnimationSet(true);
		set.setInterpolator(new DecelerateInterpolator());
		set.setFillAfter(true);
		set.setDuration(3000);

		TranslateAnimation tanim = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 2,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.3f);
		set.addAnimation(tanim);
		TranslateAnimation tanim2 = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0f,
				TranslateAnimation.RELATIVE_TO_SELF, -1f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.2f);
		tanim2.setStartOffset(1500);
		set.addAnimation(tanim2);
//		tanim.setFillAfter(true);
//		tanim.setDuration(2000);

//		Animation shake = AnimationUtils.loadAnimation(view.getContext(), R.anim.test2_shake4);
 		dice.startAnimation(set);

		mDiceHandler.sendMessageDelayed(Message.obtain(mDiceHandler, 0, 7), 200);
	}
}
