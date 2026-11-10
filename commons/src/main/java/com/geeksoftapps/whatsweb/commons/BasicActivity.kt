package com.geeksoftapps.whatsweb.commons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.preference.PowerPreference

abstract class BasicActivity: AppCompatActivity() {

    val analytics = Analytics()

    private var finishOnResume = false
    protected var onCreateTimeStamp: Long = 0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateTimeStamp = System.currentTimeMillis()
    }


    override fun onResume() {
        super.onResume()
        if (finishOnResume) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        var totalTimeSpent = PowerPreference.getDefaultFile().getLong("key_preference_time_spent_on_screens", 0)
        totalTimeSpent += ((System.currentTimeMillis() - onCreateTimeStamp)/1000)
        PowerPreference.getDefaultFile().putLong("key_preference_time_spent_on_screens", totalTimeSpent)
        super.onStop()
    }


}