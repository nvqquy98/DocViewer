package com.nvqquy98.lib.doc.widget

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.nvqquy98.lib.doc.databinding.DocWebViewBinding
import com.nvqquy98.lib.doc.interfaces.OnWebLoadListener
import timber.log.Timber

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: ProgressWebView
 * Author: Victor
 * Date: 2022/3/1 18:28
 * Description: 
 * -----------------------------------------------------------------
 */

class DocWebView : ConstraintLayout, DownloadListener {
    val TAG = "DocWebView"
    var isLastLoadSuccess = false//是否成功加载完成过web，成功过后的网络异常 不改变web
    var isError = false
    var openLinkBySysBrowser = false//是否使用系统浏览器打开http链接
    var mOnWebLoadListener: OnWebLoadListener? = null
    private val binding = DocWebViewBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initView()
    }

    fun initView() {
        with(binding) {
            mDocView.webChromeClient = DocWebChromeClient()
            mDocView.webViewClient = DocWebViewClient()
            mDocView.settings.setSupportZoom(true)
            mDocView.settings.builtInZoomControls = true
            mDocView.settings.displayZoomControls = true
            mDocView.settings.useWideViewPort = true
            mDocView.settings.loadWithOverviewMode = true
            mDocView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

            mDocView.settings.javaScriptEnabled = true
            mDocView.settings.domStorageEnabled = true
            mDocView.settings.allowFileAccess = true
            mDocView.settings.allowFileAccessFromFileURLs = true
            mDocView.settings.allowUniversalAccessFromFileURLs = true
            mDocView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

            mDocView.setDownloadListener(this@DocWebView)
        }
    }

    private fun setProgress(newProgress: Int) {
        mOnWebLoadListener?.OnWebLoadProgress(newProgress)
    }

    @SuppressLint("JavascriptInterface")
    fun addJavascriptInterface(jsInterface: Any) {
//        mDocView.addJavascriptInterface(jsInterface, "SSDJsBirdge")
    }

    fun reload() {
        isError = false
        binding.mDocView.reload()
    }

    fun loadUrl(url: String) {
        isError = false
        try {
            binding.mDocView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadData(htmlData: String) {
        binding.mDocView.loadData(htmlData, "text/html", "utf-8")
    }

    fun loadData(htmlData: String, secondLinkBySysBrowser: Boolean) {
        openLinkBySysBrowser = secondLinkBySysBrowser
        binding.mDocView.loadData(htmlData, "text/html", "utf-8")
    }

    fun downloadFile(url: String?, contentDisposition: String?, mimeType: String?) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setTitle("Completed")
//                    request.setDescription("This is description");
        request.setAllowedOverMetered(true)
        request.setVisibleInDownloadsUi(true)
        request.setAllowedOverRoaming(true)
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        Timber.e("downloadFile()-fileName = $fileName")
        request.setDestinationInExternalPublicDir(Environment.getExternalStorageDirectory().toString() + "/Download/", fileName)
        val downloadManager = binding.mDocView.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        Timber.e("onDownloadStart()......url = $url")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        context.startActivity(intent)
//        downloadFile(url,contentDisposition,mimeType)
    }

    fun canGoBack(): Boolean {
        val canGoBack = binding.mDocView.canGoBack()
        if (canGoBack) {
            binding.mDocView.goBack()
        }
        return canGoBack
    }

    fun onPause() {
        binding.mDocView.pauseTimers()
    }

    fun onResume() {
        binding.mDocView.resumeTimers()
    }

    /**
     * must be called on the main thread
     */
    fun onDestroy() {
        try {
            binding.mDocView.clearHistory();
            binding.mDocView.clearCache(true)
            binding.mDocView.loadUrl("about:blank") // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated now
            binding.mDocView.freeMemory()
            binding.mDocView.pauseTimers()
            binding.mDocView.destroy() // Note that mWebView.destroy() and mWebView = null do the exact same thing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWebViewBackgroundColor(isBlack: Boolean) {
        if (isBlack) {
            setBackgroundColor(Color.BLACK)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    private inner class DocWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            setProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            if (title.contains("html")) {
                return
            }
            mOnWebLoadListener?.onTitle(title)
        }
    }

    private inner class DocWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (!isError) {
                isLastLoadSuccess = true
                mOnWebLoadListener?.OnWebLoadProgress(100)
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
            isError = true
            if (!isLastLoadSuccess) {//之前成功加载完成过，不会回调
                mOnWebLoadListener?.OnWebLoadProgress(100)
            }
        }
    }
}
