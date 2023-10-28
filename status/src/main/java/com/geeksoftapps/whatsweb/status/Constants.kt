package com.geeksoftapps.whatsweb.status

import android.os.Environment
import java.io.File

val whatsapp_storage_file = File(Environment.getExternalStorageDirectory(), "/WhatsApp")
const val status_scoped_storage_uri = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp"
val whatsapp_saved_status_file = File(StatusBridgeProvider.getAppContext()?.filesDir, "Saved_Statuses")