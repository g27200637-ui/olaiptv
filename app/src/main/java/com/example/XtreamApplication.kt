package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.di.AppContainerImpl

class XtreamApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
