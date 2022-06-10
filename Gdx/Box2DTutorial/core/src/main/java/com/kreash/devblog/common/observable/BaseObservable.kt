package com.kreash.devblog.common.observable

interface BaseObservable<ListenerType> {

    fun registerListener(listener: ListenerType)

    fun unregisterListener(listener: ListenerType)

    fun unregisterListeners()

}