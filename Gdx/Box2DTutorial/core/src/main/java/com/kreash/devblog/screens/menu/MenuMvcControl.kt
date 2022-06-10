package com.kreash.devblog.screens.menu

import com.badlogic.gdx.Screen
import com.kreash.devblog.common.nav.SystemNav
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.nav.ScreenNav

class MenuMvcControl(
    private val mvcView: MenuMvcView,
    private val systemNav: SystemNav,
    private val screenNav: ScreenNav
) : MvcControl, MenuMvcView.Listener {

    init {
        mvcView.registerListener(this)
    }

    // region MvcControl

    override val screen: Screen
        get() = mvcView.screen

    // endregion MvcControl

    // region MenuMvcView.Listener

    override fun onNewGameClicked() {
        screenNav.navigateTo(ScreenNav.Destination.MAIN)
    }

    override fun onPreferencesClicked() {
        screenNav.navigateTo(ScreenNav.Destination.PREFERENCES)
    }

    override fun onExitClicked() {
        systemNav.exit()
    }

    override fun onDisposed() {
        mvcView.unregisterListeners()
    }

    // endregion MenuMvcView.Listener

}