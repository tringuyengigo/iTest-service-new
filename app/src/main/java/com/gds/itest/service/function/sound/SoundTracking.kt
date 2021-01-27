package com.gds.itest.service.function.sound

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.VibrationEffect
import android.os.Vibrator
import com.gds.itest.App.Companion.context
import com.gds.itest.R
import com.gds.itest.utils.AudioControl
import com.gds.itest.utils.Constants
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class SoundTracking(context: Context) {
    //    {"status":"start","property":{"timeout":"10","frequency":"3000"},"function_key":"F18_HPHONE","function_name":"Headphone"}
    //    {"status":"start","property":{"timeout":"10", "frequency":999999},"function_key":"F17_SPEAKER","function_name":"Speaker"}
    //    {"status":"start","property":{"timeout":"10", "frequency":999999},"function_key":"F20_EARPHONE","function_name":"Earphone"}
    //    {"status":"start","property":{"timeout":"10" },"function_key":"F19_MICROPHONE","function_name":"Microphone"}
    //    {"status":"start","property":{"timeout":"10" },"function_key":"F55_HJACK","function_name":"Head Jack"}
    //    { "function_key" : "F5_RECORDBACK", "property" : { "timeout" : "10" , "frequency":999999}, "status" : "start" }
    //    {"status":"start","property":{"timeout":"10"},"function_key":"F21_VIBRATION","function_name":"Vibration"}
    //    { "function_key" : "F103_VIBRATION_MIC", "function_name" : "Vibration & Microphone", "property" : { "timeout" : "10" }, "status" : "start" }
    private val mContext = context
    private val disposable = CompositeDisposable()
    private var plugged = false
    private var mp: MediaPlayer? = null
    private var mr: MediaRecorder? = null
    private var vib: Vibrator? = mContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var counterPlugged = 0L
    private var vb_t: Vibrator? = null

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            plugged = intent?.getIntExtra("state", 0) == 1
            counterPlugged++
            Timber.e("Headset plugged $plugged - $counterPlugged")
        }
    }

    init {
        mContext.registerReceiver(mBroadcastReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    fun checkHeadphone(frequency: Int,
                       timeout: Int,
                       volume: Int): Single<Pair<Boolean, String?>> =
            Single.just(frequency to timeout)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        var counter = 0
                        while (!plugged && counter++ < it.second) {
                            Timber.e("waiting headphone plug $plugged $counter")
                            Thread.sleep(1_000)
                        }
                        if (!plugged) Single.just(false to Constants.ERROR_CODE_HJACT_UNPLUGED)
                        else playSound(frequency, volume)
                    }.observeOn(AndroidSchedulers.mainThread())

    fun checkEarPhone(frequency: Int,
                      timeout: Int,
                      volume: Int): Single<Pair<Boolean, String?>> =
            Single.just(frequency to timeout)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        if (plugged) Single.just(false to Constants.ERROR_CODE_HJACT_PLUGED)
                        else {
                            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                            audioManager.isSpeakerphoneOn = false
                            playSound(frequency, volume).map { result ->
                                audioManager.mode = AudioManager.MODE_NORMAL
                                audioManager.isSpeakerphoneOn = true
                                result
                            }
                        }
                    }.observeOn(AndroidSchedulers.mainThread())

    fun checkSpeaker(frequency: Int,
                     timeout: Int,
                     volume: Int): Single<Pair<Boolean, String?>> =
            Single.just(frequency to timeout)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        if (plugged) Single.just(false to Constants.ERROR_CODE_HJACT_PLUGED)
                        else playSound(frequency, volume)
                    }.observeOn(AndroidSchedulers.mainThread())

    fun checkMicrophone(volume: Int,
                        fileName: String,
                        timeout: Int,
                        isCheckMicro: Boolean = true,
                        mic: String): Single<Pair<Boolean, String?>> =
            Single.just(fileName)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        if (plugged) Single.just(false to Constants.ERROR_CODE_HJACT_PLUGED)
                        else recordAudio(volume = volume,
                                duration = timeout,
                                fileName = fileName,
                                isCheckMicro = isCheckMicro,
                                mic = mic).map { result ->
                            if (File(fileName).exists()) result
                            else false to Constants.ERROR_CODE_AUDIO_RECORD_CAN_NOT_CREATE_FILE
                        }
                    }.observeOn(AndroidSchedulers.mainThread())

    fun checkHeadphoneMicrophone(volume: Int,
                                 fileName: String,
                                 timeout: Int,
                                 isCheckMicro: Boolean = true,
                                 mic: String): Single<Pair<Boolean, String?>> =
            Single.just(fileName)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        if (!plugged) Single.just(false to Constants.ERROR_CODE_HJACT_UNPLUGED)
                        else recordAudio(volume = volume,
                                fileName = fileName,
                                duration = timeout,
                                isCheckMicro = isCheckMicro,
                                mic = mic).map { result ->
                            if (File(fileName).exists()) result
                            else false to Constants.ERROR_CODE_AUDIO_RECORD_CAN_NOT_CREATE_FILE
                        }
                    }.observeOn(AndroidSchedulers.mainThread())

    fun checkVibrationMicrophone(volume: Int,
                                 fileName: String,
                                 timeout: Int,
                                 isCheckMicro: Boolean = true,
                                 mic: String): Single<Pair<Boolean, String?>> =
            Single.just(fileName)
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        if (plugged) Single.just(false to Constants.ERROR_CODE_HJACT_UNPLUGED)
                        else {
                            startVibration()
                            recordAudio(volume = volume,
                                    duration = timeout,
                                fileName = fileName,
                                isCheckMicro = isCheckMicro,
                                mic = mic).map { result ->
                            if (File(fileName).exists()) result
                            else false to Constants.ERROR_CODE_AUDIO_RECORD_CAN_NOT_CREATE_FILE
                            }
                        }
                    }.observeOn(AndroidSchedulers.mainThread())

    private fun startVibration() {
        vb_t = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vb_t!!.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                var vibrationEffect: VibrationEffect? = null
                vibrationEffect = VibrationEffect.createOneShot(Constants.FUNCTION_DURATION_DEFAULT, 255)
                vb_t!!.vibrate(vibrationEffect)
            } else {
                vb_t!!.vibrate(Constants.FUNCTION_DURATION_DEFAULT)
            }
        } else {
            false to Constants.ERROR_CODE_INIT_VIBRATION
        }

    }

    fun checkHJack(timeout: Int): Single<Pair<Boolean, String?>> =
            Single.just(timeout)
                    .subscribeOn(Schedulers.io())
                    .map {
                        var counter = 0
                        while (counterPlugged < 3 && counter++ < it) {
                            Thread.sleep(1_000)
                        }
                        counterPlugged
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .map {
                        if (it < 2) false to Constants.ERROR_CODE_TIME_OUT
                        else true to null
                    }


    fun checkRecordBack(frequency: Int,
                        timeout: Int,
                        volume: Int,
                        fileName: String,
                        mic: String): Single<Pair<Boolean, String?>> =

            Single.just(plugged)
                    .flatMap {
                        if (it) Single.just(false to Constants.ERROR_CODE_HJACT_PLUGED)
                        else recordAudio(volume = volume, fileName = fileName, duration = timeout, mic = mic).zipWith(
                                playSound(frequency, volume, duration = timeout.toLong()), BiFunction { data1, data2 ->
                            val first = data1.first && data2.first
                            val second = if (first) "" else "${data1.second}${data2.second}"
                            return@BiFunction (first to second)
                        })
                    }.map {
                        Timber.e("checkRecordBack finished ${it.first} ${it.second}")
                        it
                    }



    private val longVib = 1_000L
    private val longOff = 1_000L
    private val hwPattern = longArrayOf(400, 300, 400, 2000, 400, 2000, 400, 200, 2000, 200, 400) //1 cycle
    private val pattern = longArrayOf(0, longVib, longOff, longVib, longOff, longVib, longOff, longVib, longOff, longVib, longOff)
    private val mAmplitudesArr = intArrayOf(255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
    fun checkVibration(timeOut: Int): Single<Pair<Boolean, String?>> =
            Single.just(timeOut)
                    .flatMap {
                        if (vib == null) Single.just(false to Constants.ERROR_CODE_INIT_VIBRATION)
                        else vibrationDevice(it)
                    }

    private fun playSound(frequency: Int,
                          volume: Int,
                          duration: Long = Constants.FUNCTION_DURATION_DEFAULT): Single<Pair<Boolean, String?>> =
            Single.just(frequency)
                    .map {
                        Timber.e("Start playSound $duration" )
                        setStreamVolume(volume)
                        mp = MediaPlayer.create(mContext, createSoundFile(frequency))
                    }
                    .delay(1, TimeUnit.SECONDS)
                    .map { mp?.start() }
                    .delay(duration, TimeUnit.MILLISECONDS)
                    .map {
                        mp?.stop()
                        mp?.release()
                        mp = null
                        Timber.e("playSound finished")
                        true to null
                    }

    private fun recordAudio(volume: Int,
                            duration: Int = Constants.FUNCTION_DURATION_DEFAULT.toInt(),
                            fileName: String,
                            isCheckMicro: Boolean = true,
                            mic: String): Single<Pair<Boolean, String?>> =
            Single.just(volume)
                    .map {
                        Timber.e("Start recordAudio duration $duration")
                        setStreamVolume(it)
                        mr = MediaRecorder().apply {
                            if (isCheckMicro) {
                                if (mic == Constants.FUNCTION_MIC_TYPE_BACK) {
                                    setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                                } else {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                }
                                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            } else {
                                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            }
                            setAudioChannels(2)
                            setAudioSamplingRate(44100)
                            setAudioEncodingBitRate(96000)
                            setOutputFile(fileName)
                            if (isCheckMicro)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            else
                                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                            prepare()
                            start()
                        }
                    }
                    .delay(duration.toLong(), TimeUnit.MILLISECONDS)
                    .map {
                        mr?.stop()
                        mr?.release()
                        mr = null
                        Timber.e("recordAudio finished")
                        true to null
                    }

    private fun vibrationDevice(timeout: Int): Single<Pair<Boolean, String?>> =
            Single.just(timeout)
                    .map {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val vibrationEffect = VibrationEffect.createWaveform(createPattern(it), createAmpl(it), -1)
                            vib?.vibrate(vibrationEffect)
                        } else {
                            vib?.vibrate(createPattern(it), -1)
                        }
                    }
                    .delay(timeout.toLong(), TimeUnit.SECONDS)
                    .map { true to null }

    private fun createPattern(timeout: Int): LongArray {
        val pattern = LongArray(timeout + 1)
        pattern[0] = 0
        for (i in 1 until pattern.size) {
            pattern[i] = hwPattern[(i - 1) % 6]
        }
        Timber.e("pattern $pattern")
        return pattern
    }

    private fun createAmpl(timeout: Int): IntArray {
        val mAmplitude = IntArray(timeout + 1)
        mAmplitude[0] = 0
        for (i in 1 until mAmplitude.size) {
            mAmplitude[i] = mAmplitudesArr[(i - 1) % 6]
        }
        Timber.e("mAmplitude $mAmplitude")
        return mAmplitude
    }

    private fun setStreamVolume(value: Int) {
        AudioControl.setStreamVolume(AudioManager.STREAM_SYSTEM, value)
        AudioControl.setStreamVolume(AudioManager.STREAM_MUSIC, value)
    }

    @SuppressLint("NewApi")
    private val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private fun createSoundFile(frequency: Int, isCheckEarphone: Boolean? = false, isCheckHeadphone: Boolean? = false): Int {
        return when (frequency) {
            200 -> R.raw.file_200
            300 -> if (isCheckHeadphone == true) R.raw.detect_distortion_headphone else R.raw.file_300
            440 -> R.raw.file_440
            500 -> when {
                isCheckEarphone == true -> R.raw.file_500
                isCheckHeadphone == true -> R.raw.detect_distortion_headphone
                else -> R.raw.file_500
            }
            660 -> R.raw.file_660
            880 -> R.raw.file_880
            1000 -> when (isCheckHeadphone) {
                true -> R.raw.detect_distortion_headphone
                else -> R.raw.file_1000
            }
            1100 -> R.raw.file_1100
            1320 -> R.raw.file_1320
            1540 -> R.raw.file_1540
            1760 -> R.raw.file_1760
            1980 -> R.raw.file_1980
            2000 -> when (isCheckHeadphone) {
                true -> R.raw.detect_distortion_headphone
                else -> R.raw.file_2000
            }
            2200 -> R.raw.file_2200
            2420 -> R.raw.file_2420
            2540 -> R.raw.file_2540
            2640 -> R.raw.file_2640
            2860 -> R.raw.file_2860
            3080 -> R.raw.file_3080
            3300 -> R.raw.file_3300
            3520 -> R.raw.file_3520
            3740 -> R.raw.file_3740
            3000 -> R.raw.left_distortion
            4000 -> R.raw.right_none_distortion
            5000 -> R.raw.right_distortion
            999_999 -> when {
                isCheckEarphone == true -> R.raw.detect_distortion_earphone
                isCheckHeadphone == true -> R.raw.detect_distortion_headphone_bk_moi_nhat
                else -> R.raw.detect_distortion
            }
            else -> R.raw.sayhello
        }
    }

    fun unregisterHeadphone() {
        try {
            mContext.unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}