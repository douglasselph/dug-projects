package com.kreash.devblog.common.nav

import com.badlogic.gdx.Gdx

class SystemNavImpl : SystemNav {

    // region SystemNav

    override fun exit() {
        Gdx.app.exit()
    }

    // endregion SystemNav
}