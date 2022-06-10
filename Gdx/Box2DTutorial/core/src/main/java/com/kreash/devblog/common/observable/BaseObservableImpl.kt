package com.kreash.devblog.common.observable

import java.util.*

abstract class BaseObservableImpl<ListenerType> : BaseObservable<ListenerType> {

    private val _listeners = HashSet<ListenerType>()

    protected val listeners: Set<ListenerType>
        get() = Collections.unmodifiableSet(HashSet(_listeners))

    override fun registerListener(listener: ListenerType) {
        _listeners.remove(listener)
        _listeners.add(listener)
    }

    override fun unregisterListener(listener: ListenerType) {
        _listeners.remove(listener)
    }

    override fun unregisterListeners() {
        _listeners.clear()
    }

}
