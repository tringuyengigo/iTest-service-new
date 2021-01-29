package com.gds.itest.service.function.video

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import com.gds.itest.R
import com.gds.itest.interactor.FuncResultCallback
import com.gds.itest.model.Request
import com.gds.itest.utils.Constants
import com.gds.itest.utils.fromJson
import com.google.gson.Gson
import timber.log.Timber
import java.util.*


class VideoRecordServiceV1 : Service(), SurfaceHolder.Callback {
    private val TAG = VideoRecordServiceV1::class.qualifiedName
    private var mFuncResultCallback: FuncResultCallback? = null
    private var windowManager: WindowManager? = null
    private var surfaceView: SurfaceView? = null
    private var camera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    private var request: Request? = null
    private val binder = LocalBinder()


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): VideoRecordServiceV1 = this@VideoRecordServiceV1
    }

    fun setFuncResultCallback(mFuncResultCallback: FuncResultCallback) {
        Timber.e("VideoRecordService setFuncResultCallback")
        this.mFuncResultCallback = mFuncResultCallback
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.e("VideoRecordService is onStartCommand")
        parseDataFromIntent(intent)
        return START_NOT_STICKY
    }

    private fun parseDataFromIntent(intent: Intent) {
        Timber.tag(TAG).e("parseDataIntent!")
        try {
            request = intent.getStringExtra(Constants.REQUEST_PACKAGE)?.let { Gson().fromJson(it) }
            Timber.tag(TAG).e("parseDataIntent! request: $request")
        } catch (e: Exception) {
            Timber.tag(TAG).e("parseDataIntent! Exception: $e")
        }
    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        Timber.tag(TAG).e("surfaceCreated")
        when (request?.key) {
            Constants.TAG_KEYVIDEORECORD -> { startMediaRecorder(cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK, surfaceHolder = surfaceHolder) }
            Constants.TAG_KEYVIDEORECORDFRONT -> { startMediaRecorder(cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT, surfaceHolder = surfaceHolder) }
            else -> { Timber.tag(TAG).e("Function not support!!! $request") }
        }
    }

    private fun startMediaRecorder(cameraFacing: Int, surfaceHolder: SurfaceHolder) {
        try {
            Timber.tag(TAG).e("setupCamera Front[1] Back[0] cameraFacing: $cameraFacing")
            mediaRecorder = MediaRecorder()
            camera = Camera.open(cameraFacing)
            camera?.unlock()
            mediaRecorder!!.setPreviewDisplay(surfaceHolder.surface)
            mediaRecorder!!.setCamera(camera)
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
            mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P))
            mediaRecorder!!.setAudioChannels(2)
            mediaRecorder!!.setOutputFile(Constants.FILE_RESULT_PATH + "/" + request?.key +".mp4")
            try {
                mediaRecorder!!.prepare()
            } catch (ex: Exception) {
                Timber.tag(TAG).e("mediaRecorder!!.prepare() $ex")
                mFuncResultCallback?.onResultEmit(Pair(first = true, second = getString(R.string.error_media_record_start_fail)))
            }
            mediaRecorder!!.start()
        } catch (ex: Exception) {
            Timber.tag(TAG).e("startMediaRecorder $ex")
            mFuncResultCallback?.onResultEmit(Pair(first = true, second = getString(R.string.error_media_record_start_fail)))
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
    override fun surfaceDestroyed(p0: SurfaceHolder) { Timber.tag(TAG).e("VideoRecord surfaceDestroyed()") }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).e("onDestroy()")
        stopMediaRecorder()
    }

    private fun stopMediaRecorder() {
        try {
            if(mediaRecorder != null ) {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
            }
            if(camera != null ) {
                camera?.lock()
                camera?.release()
            }
            windowManager?.removeView(surfaceView);
            mFuncResultCallback?.onResultEmit(Pair(first = true, second = null))
        } catch (ex: Exception) {
            Timber.tag(TAG).e("stopMediaRecorder $ex")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).e("onCreate()")
        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        surfaceView = SurfaceView(this)
        val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.LEFT or Gravity.TOP
        windowManager!!.addView(surfaceView, layoutParams)
        surfaceView!!.holder.addCallback(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            val notification: Notification = Notification.Builder(this)
                .setContentTitle(request?.name)
                .setContentText("")
                .setSmallIcon(R.mipmap.itest_logo)
                .build()
            startForeground(1234, notification)

        }
    }


    override fun onBind(intent: Intent): IBinder {
        Timber.tag(TAG).e("onBind()")
        return binder
    }


}
