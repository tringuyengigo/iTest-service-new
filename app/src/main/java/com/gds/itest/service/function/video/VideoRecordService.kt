package com.gds.itest.service.function.video

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.gds.itest.interactor.FuncResultCallback
import timber.log.Timber

class VideoRecordService : Service() {

    // Registered callbacks
    private var mFuncResultCallback: FuncResultCallback? = null


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.e("VideoRecordService is started")
        mFuncResultCallback?.onResultEmit("PASSED")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.e("VideoRecordService is destroyed")
    }
}
