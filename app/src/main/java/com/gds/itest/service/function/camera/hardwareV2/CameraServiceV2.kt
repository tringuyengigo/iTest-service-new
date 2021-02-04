package com.gds.itest.service.function.camera.hardwareV2

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
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
import android.content.res.Resources

import android.R.attr.name
import android.hardware.Camera
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.*
import android.util.Log

class CameraServiceV2 : Service(), SurfaceHolder.Callback {
    private val TAG = "CamService"
    private val CAMERA_FRONT = "1"
    private val CAMERA_BACK = "0"
    private val CAMERA_IR = "2"
    private val binder = LocalBinder()
    private var windowManager: WindowManager? = null
    private var surfaceView: SurfaceView? = null
    private var mFuncResultCallback: FuncResultCallback? = null
    private var requestObj: Request? = null

    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var requestBuilder: CaptureRequest.Builder? = null


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
                .setContentTitle(requestObj?.name)
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
        fun getService(): CameraServiceV2 = this@CameraServiceV2
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
            requestObj = intent.getStringExtra(Constants.REQUEST_PACKAGE)?.let { Gson().fromJson(it) }
            Timber.tag(TAG).e("parseDataIntent! request: $requestObj")
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


        var camId: String? = CameraCharacteristics.LENS_FACING_BACK.toString()

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        when(requestObj?.getCameraType()?.toLowerCase(Locale.ROOT)) {
            Constants.FUNCTION_CAMERA_TYPE_BACK -> {
                camId = CAMERA_BACK
            }
            Constants.FUNCTION_CAMERA_TYPE_FRONT -> {
                camId = CAMERA_FRONT
            }
            Constants.FUNCTION_CAMERA_TYPE_IR -> {
                camId = CAMERA_IR
            }
            else -> { Timber.tag(TAG).e("Default this app will use the back camera") }
        }
        if(requestObj?.key == Constants.TAG_KEY_FRONT_IR_CAMERA) { camId = CAMERA_IR }
        Timber.tag(TAG).e("initCamera() camId: $camId CameraCharacteristics.LENS_FACING_BACK.toString() ${CameraCharacteristics.LENS_FACING_BACK}")

        previewSize = chooseSupportedSize(
            camId!!,
            textureViewWidth = 1080,
            textureViewHeight = 2280
        )
        Timber.tag(TAG).e("initCamera() previewSize $previewSize")
        cameraManager!!.openCamera(camId, stateCallback, null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun chooseSupportedSize(camId: String, textureViewWidth: Int, textureViewHeight: Int): Size? {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get all supported sizes for TextureView
        val characteristics = manager.getCameraCharacteristics(camId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val mVideoSize = chooseVideoSize(
            map!!.getOutputSizes(
                MediaRecorder::class.java
            )
        )
        mVideoSize?.let {
            chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                textureViewWidth,
                textureViewHeight,
                it
            )
        }
        return mVideoSize
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
            Timber.tag(TAG).e("onCaptureCompleted requestObj?.key = ${requestObj?.key}")
            if(requestObj?.key != Constants.TAG_KEY_FRONT_IR_CAMERA) {
                makeResult(Pair(first = true, second = null))
            }
        }
    }

    private val stateCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            Timber.tag(TAG).e("onOpened")
            cameraDevice = currentCameraDevice
            when(requestObj?.key) {
                Constants.TAG_KEY_FRONT_IR_CAMERA -> {
                    setUpMediaRecorderForIRCamera()
                }
            }
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
            when(requestObj?.key) {
                Constants.TAG_KEY_FRONT_IR_CAMERA -> {
                    // Prepare CaptureRequest that can be used with CameraCaptureSession
                    requestBuilder =
                        cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            targetSurfaces.add(mMediaRecorder!!.surface)
                            addTarget(mMediaRecorder!!.surface)
                            // Set some additional parameters for the request
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )
                        }
                }
                else -> {
                    // Prepare CaptureRequest that can be used with CameraCaptureSession
                    requestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
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
                        set(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                        )
                    }
                }
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
                            captureRequest = requestBuilder!!.build()
                            captureSession!!.setRepeatingRequest(
                                captureRequest!!,
                                captureCallback,
                                null
                            )
                        } catch (e: Exception) {
                            Timber.tag(TAG).e("createCaptureSession Exception: ${e.message}")
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
            val fileImage = File(Constants.FILE_RESULT_PATH + requestObj?.key + ".jpg")
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setUpMediaRecorderForCamera() {
        try {
            mMediaRecorder = MediaRecorder()
            mMediaRecorder?.apply {
                this.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                this.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.setOutputFile(File(Constants.FILE_RESULT_PATH + "/" + Constants.TAG_KEY_FRONT_IR_CAMERA + ".mp4"))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    previewSize?.let { mMediaRecorder!!.setVideoSize(it.width, it.height) }
                }
                this.setVideoEncodingBitRate(10000000)
                this.setVideoFrameRate(30)
                this.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                this.prepare()
                this.start()
            }
        } catch (ex: Exception) {
            Timber.tag(TAG).e("startMediaRecorder $ex")
            mFuncResultCallback?.onResultEmit(
                Pair(
                    first = true,
                    second = getString(R.string.error_media_record_start_fail)
                )
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setUpMediaRecorderForIRCamera() {
        try {
            mMediaRecorder = MediaRecorder()
            mMediaRecorder?.apply {
                this.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                this.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.setOutputFile(File(Constants.FILE_RESULT_PATH + "/" + Constants.TAG_KEY_FRONT_IR_CAMERA + ".mp4"))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    previewSize?.let { mMediaRecorder!!.setVideoSize(it.width, it.height) }
                }
                this.setVideoEncodingBitRate(10000000)
                this.setVideoFrameRate(30)
                this.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                this.prepare()
                this.start()
            }
        } catch (eException: Exception) {
            Timber.tag(TAG).e("setUpMediaRecorder => Exception: $eException")
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopRecording() {
        try {
            stopMediaRecorder()
            if (checkFileExist(File(Constants.FILE_RESULT_PATH + "/" + Constants.TAG_KEY_FRONT_IR_CAMERA + ".mp4").toString())) {
                makeResult(Pair(first = true, second = null))
            } else {
                makeResult(Pair(first = false, second = null))
            }
        } catch (mEx: Exception) {
            makeResult(Pair(first = false, second = null))
        }
    }

    private fun checkFileExist(outputFile: String): Boolean {
        val file = File(outputFile)
        if (!file.exists()) return false else {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                retriever.release()
            } catch (mException: Exception) {
                return false
            }
        }
        return true
    }

    private fun stopMediaRecorder() {
        Timber.tag(TAG).e("stopMediaRecorder()")

        if (mMediaRecorder != null) {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
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
        if(requestObj?.key == Constants.TAG_KEY_FRONT_IR_CAMERA) {
            stopRecording()
        }
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun makeResult(result: Pair<Boolean, Any?>) {
        try {
            mFuncResultCallback?.onResultEmit(result)
            stopForeground(true)
        } catch (ex: Exception) {
            Timber.tag(TAG).e("stopMediaRecorder $ex")
        }
    }


    /**
     * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size 1080p,720px
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun chooseVideoSize(choices: Array<Size>): Size? {
        for (size in choices) {
            if (1920 == size.width && 1080 == size.height) {
                return size
            }
        }
        for (size in choices) {
            if (size.width == size.height * 4 / 3 && size.width <= 1080) {
                return size
            }
        }
        return choices[choices.size - 1]
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }


    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size? {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = java.util.ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }
        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            choices[0]
        }
    }

}
