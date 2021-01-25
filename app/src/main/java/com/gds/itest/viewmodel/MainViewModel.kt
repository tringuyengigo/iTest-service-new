package com.gds.itest.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gds.itest.App
import com.gds.itest.model.Request
import com.gds.itest.utils.Constants
import com.gds.itest.utils.JsonConverter
import com.gds.itest.interactor.Event
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File

class MainViewModel : ViewModel() {

    private val disposable = CompositeDisposable()

    private val _curFunction = MutableLiveData<Event<Request>>()

    init {
        createReadingRequest()
    }


    private fun beginCheckFunction(request: Request?) {
        isChecking = false
        if (request == null) {
            createReadingRequest()
            return
        } else {
            _curFunction.value = Event(request)
            when (request.key) {
                Constants.TAG_KEYCAMERA -> checkCamera()
                else -> {
                    Timber.e("Function not support!!! ${request.key}")
                    createReadingRequest()
                }
            }
        }
    }

    private fun checkCamera() {
        TODO("Not yet implemented")
    }

    private var isChecking = false
    private fun createReadingRequest() {
        disposable.addAll(createThreadCatchFile(Constants.FILE_REQUEST_PATH)
            .map {
                /* debug*/
                App.createLog(it, 0)
                JsonConverter.convertRequest(it)
            }
            .subscribe({ beginCheckFunction(it) }, {
                Timber.e("Skip data cause [${it.message}]")
                beginCheckFunction(null)
            })
        )
    }

    private fun createThreadCatchFile(fileName: String): Single<String> =
        Single.just(fileName)
            .subscribeOn(Schedulers.io())
            .map {
                val file = File(it)
                while (file.exists().not() || file.canRead().not()) {
                    Thread.sleep(300L)
                }
                file.readText().also { file.delete() }
            }
            .observeOn(AndroidSchedulers.mainThread())
}