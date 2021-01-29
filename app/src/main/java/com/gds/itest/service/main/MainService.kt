package com.gds.itest.service.main

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.gds.itest.App
import com.gds.itest.App.Companion.context
import com.gds.itest.R
import com.gds.itest.interactor.Event
import com.gds.itest.interactor.FuncResultCallback
import com.gds.itest.model.Request
import com.gds.itest.service.function.camera.CameraService
import com.gds.itest.service.function.sound.SoundTracking
import com.gds.itest.service.function.video.VideoRecordServiceV1
import com.gds.itest.utils.Constants
import com.gds.itest.utils.FileUtils
import com.gds.itest.utils.JsonConverter
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.util.*


class MainService : Service(), FuncResultCallback {

    private val TAG = MainService::class.qualifiedName
    private var mBound: Boolean = false
    private val disposable = CompositeDisposable()
    private val _curFunction = MutableLiveData<Event<Request>>()
    private var soundTracking: SoundTracking? = null
    private var notification: Notification? = null
    private var mServiceV1: Any? = null
    private var isChecking = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand!")
        soundTracking = SoundTracking(context)
        createReadingRequest()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).e("onDestroy!")
        unbindService(connection)
        mBound = false
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Timber.tag(TAG).e("onServiceConnected!")
            selectServiceConnected(service)
            mBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private fun selectServiceConnected(service: IBinder) {
        when (_curFunction.value?.peekContent()?.key) {
            Constants.TAG_KEYVIDEORECORD, Constants.TAG_KEYVIDEORECORDFRONT -> {
                val binder = service as VideoRecordServiceV1.LocalBinder
                mServiceV1 = binder.getService()
                (mServiceV1 as VideoRecordServiceV1).setFuncResultCallback(this@MainService)
            }
            Constants.TAG_KEYCAMERA -> {
                val binder = service as CameraService.LocalBinder
                mServiceV1 = binder.getService()
                (mServiceV1 as CameraService).setFuncResultCallback(this@MainService)
            }
            else -> {
                Timber.tag(TAG).e("Function not support!!! ${_curFunction.value?.peekContent()?.key}")
                createReadingRequest()
            }
        }
    }

    private fun createReadingRequest() {
        Timber.tag(TAG).e("createReadingRequest!")
        if (!isChecking) {
            isChecking = true
            disposable.addAll(createThreadCatchFile(Constants.FILE_REQUEST_PATH)
                .map {
                    Timber.tag(TAG).e("createReadingRequest json:  $it")
                    JsonConverter.convertRequest(it)
                }
                .doOnError {
                    Timber.tag(TAG).e("createReadingRequest doOnError: $it")
                }
                .subscribe({ beginCheckFunction(it) }, {
                    Timber.tag(TAG).e("Skip data cause [${it.message}]")
                    beginCheckFunction(null)
                })
            )
        }
    }

    private fun createThreadCatchFile(fileName: String): Single<String> =
        Single.just(fileName)
            .subscribeOn(Schedulers.io())
            .map {
                Timber.tag(TAG).e("createThreadCatchFile fileName [${it}]")
                val file = File(it)
                while (file.exists().not() || file.canRead().not()) {
                    Timber.e("createThreadCatchFile waiting to catch file")
                    Thread.sleep(300L)
                }
                file.readText().also { file.delete() }
            }
            .doOnError {
                Timber.tag(TAG).e("createThreadCatchFile doOnError: $it")
            }
            .observeOn(AndroidSchedulers.mainThread())


    @SuppressLint("NewApi")
    private fun beginCheckFunction(request: Request?) {
        Timber.tag(TAG).e("beginCheckFunction request: [${request}]")
        isChecking = false
        if (request == null) {
            createReadingRequest()
            return
        } else {
            startNotificationForeground(request)
            _curFunction.value = Event(request)
            when (request.key) {
                Constants.TAG_KEYVIDEORECORD -> {
                    when(checkCamerasExist(CameraMetadata.LENS_FACING_BACK)) {
                            true -> checkCheckVideoRecord(request = request)
                            false -> onResultEmit(Pair(first = false, second = getString(R.string.error_cannot_get_back_camera_id)))
                    }
                }
                Constants.TAG_KEYVIDEORECORDFRONT -> {
                    when(checkCamerasExist(CameraMetadata.LENS_FACING_FRONT)) {
                            true -> checkCheckVideoRecord(request = request)
                            false -> onResultEmit(Pair(first = false, second = getString(R.string.error_cannot_get_back_camera_id)))
                        }
                }
                Constants.TAG_KEYCAMERA -> {
                    when (request.getCameraType()) {
                        Constants.FUNCTION_CAMERA_TYPE_BACK -> {
                            when(checkCamerasExist(CameraMetadata.LENS_FACING_BACK)) {
                                true -> checkCameras(request = request)
                                false -> onResultEmit(Pair(first = false, second = getString(R.string.error_cannot_get_back_camera_id)))
                            }
                        }
                        Constants.FUNCTION_CAMERA_TYPE_FRONT ->{
                            when(checkCamerasExist(CameraMetadata.LENS_FACING_FRONT)) {
                                true -> checkCameras(request = request)
                                false -> onResultEmit(Pair(first = false, second = getString(R.string.error_cannot_get_back_camera_id)))
                            }
                        }
                    }
                }
                Constants.TAG_KEYMICROPHONE -> checkMicrophones(request = request)
                else -> {
                    Timber.tag(TAG).e("Function not support!!! ${request.key}")
                    createReadingRequest()
                }
            }
        }
    }

    private fun startNotificationForeground(request: Request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = request.getImageResult()?.let {
                Notification.Builder(this)
                    .setContentTitle(request.name)
                    .setContentText("")
                    .setSmallIcon(it)
                    .build()
            }
            startForeground(12345, notification)
        }
    }

    private fun checkMicrophones(request: Request) {
        Timber.tag(TAG).e("Start checkMicrophones")
        disposable.addAll(Single.just(_curFunction.value?.peekContent())
            .flatMap {
                soundTracking?.checkMicrophone(
                    volume = it.getVolume(),
                    fileName = Constants.FILE_RESULT_PATH.plus(it.key).plus(
                        if (it.key.equals(Constants.TAG_KEYMICROPHONE, true))
                            ".3gp" else ".m4a"
                    ),
                    timeout = it.getTimeOut() * 1000,
                    isCheckMicro = true,
                    mic = it.getMic()
                )
            }.subscribe({ result ->
                makeResult(result.first ?: false, result.second)
            }, {
                Timber.e("Err ${it.message}")
                makeResult(false, it.message)
            })
        )
    }

    private fun checkCameras(request: Request) {
        Timber.tag(TAG).e("Start checkCameras")
        try {
            val mIntent = Intent(this, CameraService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            mIntent.putExtra(Constants.REQUEST_PACKAGE, Gson().toJson(request))
            startTestFunctionService(mIntent = mIntent)
            request.getTimeToRecord().toLong().let { stopTestFunctionService(
                mIntent = mIntent,
                delay = it
            ) }
        } catch (mEx: Exception) {
            Timber.tag(TAG).e("Exception: $mEx")
        }
    }


    private fun checkCheckVideoRecord(request: Request?) {
        Timber.tag(TAG).e("Start checkCheckVideoRecord")
        try {
            val mIntent = Intent(this, VideoRecordServiceV1::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            mIntent.putExtra(Constants.REQUEST_PACKAGE, Gson().toJson(request))
            startTestFunctionService(mIntent = mIntent)
            request?.getTimeToRecord()?.toLong()?.let { stopTestFunctionService(
                mIntent = mIntent,
                delay = it
            ) }
        } catch (mEx: Exception) {
            Timber.tag(TAG).e("Exception: $mEx")
        }
    }

    private fun startTestFunctionService(mIntent: Intent) {
        Timber.tag(TAG).e("startTestFunctionService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(mIntent)
        }
    }

    private fun stopTestFunctionService(mIntent: Intent, delay: Long) {
        Timber.tag(TAG).e("stopTestFunctionService")
        try {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    Timber.tag(TAG).e("Stop service")
                    context.stopService(mIntent)
                    unbindService(connection)
                    mBound = false
                }
            }, delay * 1000)
        } catch (mEx: Exception) {
            Timber.tag(TAG).e("stopTestFunctionService Exception: $mEx")
        }

    }

    override fun onResultEmit(result: Pair<Boolean, Any?>) {
        Timber.tag("MainService").e("onResultEmit result: $result")
        makeResult(result.first ?: false, result.second)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkCamerasExist(idCamera: Int) : Boolean {
        Timber.tag(TAG).e("checkCamerasExist => ")
        var backCameraId: String? = null
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        for (cameraId in manager.cameraIdList) {
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == idCamera) {
                backCameraId = cameraId
                break
            }
        }
        Timber.tag(TAG).e("checkCamerasExist => ${(backCameraId!=null)}")
        return (backCameraId!=null)
    }

    private fun makeResult(isPass: Boolean, note: Any? = null) {
        _curFunction.value?.peekContent()?.apply {
            result = if (isPass) Constants.PASSED else Constants.FAILED
            setNote(note)
        }?.let {
            Timber.tag("MainService").e("writeResultFile $it")
            App.createLog(it.toString(), 2)
            FileUtils.writeResultFile(it)
        }
        stopForeground(true)
        createReadingRequest()
    }




}


