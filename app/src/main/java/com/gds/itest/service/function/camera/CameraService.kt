package com.gds.itest.service.function.camera

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Size
import android.view.*
import androidx.annotation.RequiresApi
import com.gds.itest.R
import com.gds.itest.interactor.FuncResultCallback
import com.gds.itest.model.Request
import com.gds.itest.utils.Constants
import com.gds.itest.utils.fromJson
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

class CameraService : Service(), SurfaceHolder.Callback {
    val TAG = "CamService"
    private val binder = LocalBinder()
    private var windowManager: WindowManager? = null
    private var surfaceView: SurfaceView? = null
    private var mFuncResultCallback: FuncResultCallback? = null
    private var request: Request? = null

    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val notification: Notification = Notification.Builder(this)
                .setContentTitle(request?.name)
                .setSmallIcon(R.mipmap.itest_logo)
                .build()
            startForeground(1234, notification)
        }
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): CameraService = this@CameraService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun surfaceCreated(p0: SurfaceHolder) {
        initCamera()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initCamera() {
        Timber.tag(TAG).e("initCamera() request?.getCameraType(): ${request?.getCameraType()}")
        var camId: String? = CameraCharacteristics.LENS_FACING_BACK.toString()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        when(request?.getCameraType()){
            Constants.FUNCTION_CAMERA_TYPE_BACK -> { camId = CameraCharacteristics.LENS_FACING_BACK.toString() }
            Constants.FUNCTION_CAMERA_TYPE_FRONT -> { camId = CameraCharacteristics.LENS_FACING_FRONT.toString() }
            Constants.FUNCTION_CAMERA_TYPE_IR -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    camId = CameraCharacteristics.LENS_FACING_EXTERNAL.toString()
                }
            }
            else -> { Timber.tag(TAG).e("Default this app will use the back camera") }
        }

        previewSize = chooseSupportedSize(camId!!, textureViewWidth = 1200, textureViewHeight = 1600)
        cameraManager!!.openCamera(camId, stateCallback, null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun chooseSupportedSize(camId: String, textureViewWidth: Int, textureViewHeight: Int): Size {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get all supported sizes for TextureView
        val characteristics = manager.getCameraCharacteristics(camId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)

        // We want to find something near the size of our TextureView
        val texViewArea = textureViewWidth * textureViewHeight
        val texViewAspect = textureViewWidth.toFloat()/textureViewHeight.toFloat()

        val nearestToFurthestSz = supportedSizes?.sortedWith(compareBy(
                // First find something with similar aspect
                {
                    val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                    else it.height.toFloat() / it.width.toFloat()
                    (aspect - texViewAspect).absoluteValue
                },
                // Also try to get similar resolution
                {
                    (texViewArea - it.width * it.height).absoluteValue
                }
        ))
        Timber.tag(TAG).e("chooseSupportedSize => nearestToFurthestSz[0] ${nearestToFurthestSz?.get(0)?.height}")
        if (nearestToFurthestSz?.isNotEmpty() == true) return nearestToFurthestSz[0]
        return Size(600*2, 800*2)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
    override fun surfaceDestroyed(p0: SurfaceHolder) { Timber.tag(TAG).e("VideoRecord surfaceDestroyed()") }

    private val captureCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {}

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            Timber.tag(TAG).e("onCaptureCompleted")
            makeResult(Pair(first = true, second = null))
        }
    }

    private val stateCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            Timber.tag(TAG).e("onOpened")
            cameraDevice = currentCameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            Timber.tag(TAG).e("onDisconnected")
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            Timber.tag(TAG).e("onError $error")
            currentCameraDevice.close()
            cameraDevice = null
            makeResult(Pair(first = false, second = "onError $error"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createCaptureSession() {
        try {
            // Prepare surfaces we want to use in capture session
            val targetSurfaces = ArrayList<Surface>()
            // Prepare CaptureRequest that can be used with CameraCaptureSession
            val requestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                // Configure target surface for background processing (ImageReader)
                imageReader = ImageReader.newInstance(
                        previewSize!!.width, previewSize!!.height,
                        ImageFormat.JPEG, 1
                )
                imageReader!!.setOnImageAvailableListener(imageAvailableListener, null)

                targetSurfaces.add(imageReader!!.surface)
                addTarget(imageReader!!.surface)

                // Set some additional parameters for the request
                set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }

            // Prepare CameraCaptureSession
            cameraDevice!!.createCaptureSession(
                    targetSurfaces,
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return
                            }

                            captureSession = cameraCaptureSession
                            try {
                                // Now we can start capturing
                                captureRequest = requestBuilder.build()
                                captureSession!!.setRepeatingRequest(
                                        captureRequest!!,
                                        captureCallback,
                                        null
                                )
                            } catch (e: Exception) {
                                Timber.tag(TAG).e("createCaptureSession Exception: $e")
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            Timber.tag(TAG).e("onConfigureFailed")
                        }
                    }, null
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e("createCaptureSession Exception -> $e")
        }
    }

    private val imageAvailableListener: OnImageAvailableListener = @RequiresApi(Build.VERSION_CODES.KITKAT)
    object : OnImageAvailableListener {
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun onImageAvailable(reader: ImageReader) {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                Timber.tag(TAG).e("imageAvailableListener => image size: ${image.width} x ${image.height}")
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer[bytes]
                save(bytes)
            } catch (ee: Exception) {
                Timber.tag(TAG).e("imageAvailableListener => Exception: $ee")
            } finally {
                image?.close()
                Timber.tag(TAG).e("imageAvailableListener => finally")
            }
        }

        fun save(bytes: ByteArray?) {
            Timber.tag(TAG).e("save imageAvailableListener !!!!!!!")
            val fileImage = File(Constants.FILE_RESULT_PATH + request?.key + ".jpg")
            var outputStream: OutputStream? = null
            try {
                outputStream = FileOutputStream(fileImage)
                outputStream.write(bytes)
            } catch (e: Exception) {
                Timber.tag(TAG).e("imageAvailableListener => Exception => save $e")
            } finally {
                try {
                    outputStream?.close()
                } catch (e: java.lang.Exception) {
                    Timber.tag(TAG).e("imageAvailableListener => Exception => finally save $e")
                }
            }
        }
    }

    fun setFuncResultCallback(mFuncResultCallback: FuncResultCallback) {
        Timber.e("VideoRecordService setFuncResultCallback")
        this.mFuncResultCallback = mFuncResultCallback
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).e("onDestroy()")
        stopCamera()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeResult(result: Pair<Boolean, Any?>) {
        try {
            mFuncResultCallback?.onResultEmit(result)
            stopForeground(true)
        } catch (ex: Exception) {
            Timber.tag(TAG).e("stopMediaRecorder $ex")
        }
    }

}
