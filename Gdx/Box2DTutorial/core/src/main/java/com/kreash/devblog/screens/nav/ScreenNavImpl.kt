package com.kreash.devblog.screens.nav

class ScreenNavImpl : ScreenNav {

    private var hook: ScreenNav.Hook = object : ScreenNav.Hook {
        override fun toScreen(dest: ScreenNav.Destination) {
        }
    }

    // region ScreenNav

    override fun installHook(hook: ScreenNav.Hook) {
        this.hook = hook
    }

    override fun navigateTo(dest: ScreenNav.Destination) {
        hook.toScreen(dest)
    }

    // endregion ScreenNav

}