package com.gds.itest.service.main

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gds.itest.App.Companion.context
import com.gds.itest.interactor.Event
import com.gds.itest.interactor.FuncResultCallback
import com.gds.itest.model.Request
import com.gds.itest.service.function.video.VideoRecordService
import com.gds.itest.utils.Constants
import com.gds.itest.utils.JsonConverter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File


class MainService : Service(), FuncResultCallback {

    private var mService: VideoRecordService? = null
    private var mBound: Boolean = false

    private val disposable = CompositeDisposable()
    private val _curFunction = MutableLiveData<Event<Request>>()
    val curFunction: LiveData<Event<Request>> = _curFunction

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show()
        createReadingRequest()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Service destroyed by user.", Toast.LENGTH_LONG).show()
    }

    private var isChecking = false
    private fun createReadingRequest() {
        Timber.e("createReadingRequest $isChecking")
        if (!isChecking) {
            isChecking = true
            disposable.addAll(createThreadCatchFile(Constants.FILE_REQUEST_PATH)
                .map {
                    Timber.e("createReadingRequest json: $it")
                    JsonConverter.convertRequest(it)
                }
                .doOnError {
                    Timber.e("createReadingRequest doOnError: $it")
                }
                .subscribe({ beginCheckFunction(it) }, {
                    Timber.e("Skip data cause [${it.message}]")
                    beginCheckFunction(null)
                })

            )
        }
    }

    private fun createThreadCatchFile(fileName: String): Single<String> =
        Single.just(fileName)
            .subscribeOn(Schedulers.io())
            .map {
                Timber.e("createThreadCatchFile fileName [${it}]")
                val file = File(it)
                while (file.exists().not() || file.canRead().not()) {
                    Timber.e("createThreadCatchFile waiting to catch file")
                    Thread.sleep(300L)
                }
                file.readText().also { file.delete() }
            }
            .doOnError {
                Timber.e("createThreadCatchFile doOnError: $it")
            }
            .observeOn(AndroidSchedulers.mainThread())


    private fun beginCheckFunction(request: Request?) {
        isChecking = false
        if (request == null) {
            createReadingRequest()
            return
        } else {
            _curFunction.value = Event(request)
            when (request.key) {
                Constants.TAG_KEYVIDEORECORD -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        checkCheckVideoRecord()
                    }
                }
                else -> {
                    Timber.e("Function not support!!! ${request.key}")
                    createReadingRequest()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkCheckVideoRecord() {
        Timber.e("start checkCheckVideoRecord")
        try {
            context.startService(Intent(context, VideoRecordService::class.java))
        } catch (mEx: Exception) {
            Timber.e("checkCheckVideoRecord Exception => start service ")
        }
    }



    override fun onResultEmit(result: String) {
        Timber.tag("MainService").e("onResultEmit result: $result")
    }


}
