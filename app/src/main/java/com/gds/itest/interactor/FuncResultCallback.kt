package com.gds.itest.interactor

interface FuncResultCallback {
    fun onResultEmit(result: Pair<Boolean, Any?>)
}