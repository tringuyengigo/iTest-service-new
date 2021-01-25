package com.gds.itest

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import androidx.lifecycle.ProcessLifecycleOwner
import com.gds.itest.utils.Constants
import com.gds.itest.utils.formatByPattern
import com.gds.itest.interactor.AppLifecycleListening
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.util.*


/**
 * Created by man-gds on 30/03/2017.
 */

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        App.context = applicationContext
        initAppLifecycle()
        Timber.plant(Timber.DebugTree())
        getScreenSize()
        createLogging()

    }

    private fun initAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : AppLifecycleListening {})
    }

    private fun createLogging() {
        val log = logPublishSubject
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { createDir() }
                .doOnNext { createFileLog(it) }
                .subscribe({
                    Timber.d("write passed")
                }, {
                    Timber.e("write failed ${it.message}")
                })
        logPublishSubject.onNext("Application Init" to 2)
    }

    private fun createDir() {
        fileWritter = File(Constants.FILE_DEBUG_PATH)
        fileWritter.createNewFile()
    }

    @Throws(Exception::class)
    private fun createFileLog(data: Pair<String,Int>) {
        val convertedData = Calendar.getInstance().formatByPattern(DATE_PATTERN).plus(
                when(data.second) {
                    0 -> " [REQUEST] "
                    1 -> " [RESULT] "
                    else -> " [LOG] "
                }

        ).plus(data.first).plus("\n")
        fileWritter.appendText(convertedData)
    }

    @SuppressLint("NewApi")
    private fun getScreenSize() {
        val size = Point()
        (applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.getRealSize(size)
        Timber.e("Real size: $size")
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
        lateinit var fileWritter: File
            private set
        private val logPublishSubject = PublishSubject.create<Pair<String,Int>>()

        private const val DATE_PATTERN = "MM-dd-yyyy-HH-mm-ss"
        private const val FILE_DIR = "ItestLog"
        private const val FILE_SUFFIX = "_"

        /**
         * @param 0 : request, 1: result, else log
         */
        fun createLog(data: String, fileType: Int = 2){
            logPublishSubject.onNext(data to fileType)
        }
    }
}
