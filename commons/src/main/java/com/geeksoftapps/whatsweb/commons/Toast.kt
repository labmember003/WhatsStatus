package com.geeksoftapps.whatsweb.commons

import android.widget.Toast

fun toast(msg: String) = Toast.makeText(CommonsBridgeProvider.appContext, msg, Toast.LENGTH_SHORT).show()