package com.wesley.cuadrantes

import android.app.Application

class CuadrantesApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
