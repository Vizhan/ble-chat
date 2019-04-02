package com.lanars.pdpblechatapp.di

import com.lanars.pdpblechatapp.chat.vm.ChatViewModel
import com.polidea.rxandroidble2.RxBleClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val bleModule = module {
    single { RxBleClient.create(androidApplication()) }
    viewModel { ChatViewModel(androidApplication()) }
}