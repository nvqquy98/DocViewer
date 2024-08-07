package com.nvqquy98.lib.doc

import timber.log.Timber
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

object DocViewerLib {
    internal var sslSocketFactory: SSLSocketFactory? = null
    internal var hostnameVerifier: HostnameVerifier? = null

    fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
            })
        }
    }

    fun setSSlSocketFactory(sslSocketFactory: SSLSocketFactory) {
        this.sslSocketFactory = sslSocketFactory
    }

    fun setHostNameVerifier(hostnameVerifier: HostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier
    }
}
