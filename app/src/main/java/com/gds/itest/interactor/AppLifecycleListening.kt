package com.gds.itest.interactor

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber

interface AppLifecycleListening : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        Timber.e("===>onMoveToForeground")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onEventPause() {
        Timber.e("===>onEventPause")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        Timber.e("===>onMoveToBackground")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onEventDestroy() {
        Timber.e("===>onEventDestroy")
    }
}