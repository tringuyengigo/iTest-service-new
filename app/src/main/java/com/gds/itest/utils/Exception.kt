package com.gds.itest.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun Calendar.formatByPattern(pattern: String): String = SimpleDateFormat(pattern).format(this.time)
