package com.kreash.devblog.common.observable

import com.badlogic.gdx.Screen

interface MvcView<ListenerType> : BaseObservable<ListenerType> {

    val screen: Screen

}
