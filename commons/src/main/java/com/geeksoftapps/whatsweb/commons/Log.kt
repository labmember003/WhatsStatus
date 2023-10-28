package com.geeksoftapps.whatsweb.commons

import android.util.Log

fun log(message: Any?) = Log.d("LOGD", message.toString())

fun logError(message: Any?) = Log.e("LOGE", message.toString())