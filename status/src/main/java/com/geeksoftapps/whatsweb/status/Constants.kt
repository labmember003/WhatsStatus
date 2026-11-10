package com.geeksoftapps.whatsweb.status

import android.os.Environment
import java.io.File

val whatsapp_storage_file = File(Environment.getExternalStorageDirectory(), "/WhatsApp")
const val status_scoped_storage_uri = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp"
val whatsapp_saved_status_file = File(StatusBridgeProvider.getAppContext()?.filesDir, "Saved_Statuses")

val whatsapp_business_storage_file = File(Environment.getExternalStorageDirectory(), "/WhatsApp Business")
const val business_scoped_storage_uri = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b"
val whatsapp_business_saved_status_file = File(StatusBridgeProvider.getAppContext()?.filesDir, "Saved_Business_Statuses")