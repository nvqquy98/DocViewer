package com.nvqquy98.lib.doc.pdf

import android.annotation.SuppressLint
import com.nvqquy98.lib.doc.DocViewerLib
import com.nvqquy98.lib.doc.interfaces.OnDownloadListener
import com.nvqquy98.lib.doc.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: PdfDownloader
 * Author: Victor
 * Date: 2023/09/28 11:13
 * Description: 
 * -----------------------------------------------------------------
 */
const val OK_HOSTNAME_VERIFIER_CLASS = "com.android.okhttp.internal.tls.OkHostnameVerifier";

internal class PdfDownloader(url: String, private val listener: OnDownloadListener) {

    init {
        listener.getCoroutineScope().launch(Dispatchers.IO) { download(url) }
    }

    @SuppressLint("PrivateApi")
    private fun download(downloadUrl: String) {
        val format = FileUtils.getFileFormatForUrl(downloadUrl)
        listener.getCoroutineScope().launch(Dispatchers.Main) { listener.onDownloadStart() }
        val outputFile = File(listener.getDownloadContext().cacheDir, "doc.$format")
        if (outputFile.exists())
            outputFile.delete()
        try {
            val bufferSize = 8192
            val url = URL(downloadUrl)
            val connection = url.openConnection() as? HttpsURLConnection
            DocViewerLib.sslSocketFactory?.let {
                connection?.sslSocketFactory = DocViewerLib.sslSocketFactory
                connection?.setHostnameVerifier { hostname, session ->
                    DocViewerLib.hostnameVerifier?.verify(hostname, session) ?: (Class.forName(
                        OK_HOSTNAME_VERIFIER_CLASS
                    ).getField("INSTANCE")[null] as? HostnameVerifier)?.verify(hostname, session)
                    ?: false
                }
            }
            connection?.connect()

            val totalLength = connection?.contentLength ?: 0
            val inputStream = BufferedInputStream(connection?.inputStream, bufferSize)
            val outputStream = outputFile.outputStream()
            var downloaded = 0

            do {
                val data = ByteArray(bufferSize)
                val count = inputStream.read(data)
                if (count == -1)
                    break
                if (totalLength > 0) {
                    downloaded += bufferSize
                    listener.getCoroutineScope().launch(Dispatchers.Main) {
                        listener.onDownloadProgress(
                            downloaded.toLong(),
                            totalLength.toLong()
                        )
                    }
                }
                outputStream.write(data, 0, count)
            } while (true)
        } catch (e: Exception) {
            e.printStackTrace()
            listener.getCoroutineScope().launch(Dispatchers.Main) { listener.onError(e) }
            return
        }
        listener.getCoroutineScope()
            .launch(Dispatchers.Main) { listener.onDownloadSuccess(outputFile.absolutePath) }
    }
}
