package com.gds.itest.utils

import com.gds.itest.model.Request
import java.io.File

object FileUtils {
    fun readFile(path: String? = Constants.FILE_REQUEST_PATH) = File(path).readText()
    fun writeFile(path: String, data: String) = File(path).writeText(data)
    fun writeResultFile(result: Request) = File(Constants.FILE_RESULT_PATH.plus(result.key).plus(".txt")).writeText(result.createJsonResult())
    @Throws(Exception::class)
    fun writeImgFile(fileName: String, data: ByteArray) = File(fileName).writeBytes(data)
}