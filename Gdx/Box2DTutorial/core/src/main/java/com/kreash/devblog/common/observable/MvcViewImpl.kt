package com.kreash.devblog.common.observable

abstract class MvcViewImpl<ListenerType> :
    BaseObservableImpl<ListenerType>(),
    MvcView<ListenerType>
