package com.lanars.pdpblechatapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.polidea.rxandroidble2.internal.RxBleLog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        RxBleLog.setLogLevel(RxBleLog.VERBOSE)
    }
}
