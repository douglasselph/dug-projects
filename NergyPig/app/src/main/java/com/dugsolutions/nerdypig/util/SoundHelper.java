package com.dugsolutions.nerdypig.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.dugsolutions.nerdypig.R;

/**
 * Created by dug on 12/20/16.
 */

public class SoundHelper
{
	SoundPool	mDieSound	= new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
	int			mSoundId;

	SoundHelper(Context ctx)
	{
		mSoundId = mDieSound.load(ctx, R.raw.shake_dice, 1);
	}

    void play()
    {
        mDieSound.play(mSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    void pause()
    {
        mDieSound.pause(mSoundId);
    }
}
