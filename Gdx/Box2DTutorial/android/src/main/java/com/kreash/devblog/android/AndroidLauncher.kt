package com.kreash.devblog.android

import com.badlogic.gdx.backends.android.AndroidApplication
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.kreash.devblog.MainOrchestrator

/** Launches the Android application.  */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = AndroidApplicationConfiguration()
        initialize(MainOrchestrator(), configuration)
    }
}