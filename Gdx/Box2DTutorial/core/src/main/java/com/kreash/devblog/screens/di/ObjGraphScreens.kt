package com.kreash.devblog.screens.di

import com.kreash.devblog.common.di.ObjGraphCommon
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.main.MainMvcControl
import com.kreash.devblog.screens.main.MainMvcView
import com.kreash.devblog.screens.main.MainMvcViewImpl
import com.kreash.devblog.screens.main.data.ObjFactory
import com.kreash.devblog.screens.main.data.ObjFactoryImpl
import com.kreash.devblog.screens.main.data.WorldObj
import com.kreash.devblog.screens.main.data.WorldObjImpl
import com.kreash.devblog.screens.main.repo.WorldRepo
import com.kreash.devblog.screens.menu.MenuMvcControl
import com.kreash.devblog.screens.menu.MenuMvcView
import com.kreash.devblog.screens.menu.MenuMvcViewImpl
import com.kreash.devblog.screens.nav.ScreenNav
import com.kreash.devblog.screens.nav.ScreenNavImpl
import com.kreash.devblog.screens.preferences.PreferencesMvcControl
import com.kreash.devblog.screens.preferences.PreferencesMvcView
import com.kreash.devblog.screens.preferences.PreferencesMvcViewImpl
import com.kreash.devblog.screens.preferences.cases.AppPrefs
import com.kreash.devblog.screens.preferences.cases.AppPrefsImpl

class ObjGraphScreens(
    private val common: ObjGraphCommon
) {

    // NAV
    val screenNav: ScreenNav by lazy { ScreenNavImpl() }

    // MENU
    private val menuMvcView: MenuMvcView by lazy { MenuMvcViewImpl() }

    val menuMvcControl: MvcControl by lazy {
        MenuMvcControl(
            menuMvcView,
            common.systemNav,
            screenNav
        )
    }

    // PREFERENCES
    private val prefs: AppPrefs by lazy { AppPrefsImpl() }
    private val preferencesMvcView: PreferencesMvcView by lazy { PreferencesMvcViewImpl() }

    val preferencesMvcControl: PreferencesMvcControl by lazy {
        PreferencesMvcControl(
            preferencesMvcView,
            prefs,
            screenNav
        )
    }

    // MAIN
    private val worldObj: WorldObj
        get() = WorldObjImpl()
    private val mainRepo: WorldRepo by lazy { WorldRepo { worldObj } }
    private val mainMvcView: MainMvcView by lazy { MainMvcViewImpl() }

    val mainMvcControl: MainMvcControl by lazy {
        MainMvcControl(
            mainMvcView,
            mainRepo
        )
    }

}