package com.lanars.pdpblechatapp

import android.app.Application
import com.lanars.pdpblechatapp.di.bleModule
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.internal.RxBleLog
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module

class BleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin(this, provideModuleList())

        RxBleClient.setLogLevel(RxBleLog.DEBUG)
    }

    private fun provideModuleList(): List<Module> =
        listOf(
            bleModule
        )
}