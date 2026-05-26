package com.nvqquy98.lib.doc.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.blankj.utilcode.util.UriUtils
import com.nvqquy98.lib.doc.R
import com.nvqquy98.lib.doc.bean.DocEngine
import com.nvqquy98.lib.doc.bean.DocMovingOrientation
import com.nvqquy98.lib.doc.bean.DocSourceType
import com.nvqquy98.lib.doc.bean.FileType
import com.nvqquy98.lib.doc.databinding.DocViewBinding
import com.nvqquy98.lib.doc.interfaces.OnDownloadListener
import com.nvqquy98.lib.doc.interfaces.OnDocPageChangeListener
import com.nvqquy98.lib.doc.interfaces.OnPdfItemClickListener
import com.nvqquy98.lib.doc.interfaces.OnWebLoadListener
import com.nvqquy98.lib.doc.office.IOffice
import com.nvqquy98.lib.doc.pdf.PdfDownloader
import com.nvqquy98.lib.doc.pdf.PdfPageViewAdapter
import com.nvqquy98.lib.doc.pdf.PdfQuality
import com.nvqquy98.lib.doc.pdf.PdfRendererCore
import com.nvqquy98.lib.doc.pdf.PdfViewAdapter
import com.nvqquy98.lib.doc.util.Constant
import com.nvqquy98.lib.doc.util.FileUtils
import com.nvqquy98.lib.doc.util.ViewUtils.hide
import com.nvqquy98.lib.doc.util.ViewUtils.show
import timber.log.Timber
import java.io.File
import java.net.URL
import java.net.URLEncoder

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: PdfView
 * Author: Victor
 * Date: 2023/10/30 11:30
 * Description: 
 * -----------------------------------------------------------------
 */

open class DocView @JvmOverloads constructor(context: Context, private val attrs: AttributeSet? = null, private val defStyle: Int = 0) :
    FrameLayout(context, attrs, defStyle), OnDownloadListener,
    OnWebLoadListener, OnPdfItemClickListener {

    private val TAG = "DocView"
    var mActivity: Activity? = null
    var lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    private var mPoiViewer: PoiViewer? = null
    private var pdfRendererCore: PdfRendererCore? = null
    private var mMovingOrientation = DocMovingOrientation.HORIZONTAL
    private var setInterceptTouchEventListener: ((MotionEvent?) -> Boolean)? = null
    private var quality = PdfQuality.NORMAL
    private var engine = DocEngine.GOOGLE
    private var showDivider = true
    private var showPageNum = true
    private var divider: Drawable? = null
    private var runnable = Runnable {}
    var enableLoadingForPages: Boolean = true
    var pbDefaultHeight = 2
    var pbHeight: Int = pbDefaultHeight
    var pbDefaultColor = Color.RED
    var pbColor: Int = pbDefaultColor

    private var pdfRendererCoreInitialised = false
    var pageMargin: Rect = Rect(0, 0, 0, 0)

    var totalPageCount = 0

    var mOnDocPageChangeListener: OnDocPageChangeListener? = null

    var sourceFilePath: String? = null
    var mViewPdfInPage: Boolean = true

    private val binding: DocViewBinding = DocViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.mIvPdf.setOnClickListener {
            binding.mLlBigPdfImage.hide()
        }
        initView()
    }

    fun initView() {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DocView, defStyle, 0)
        try {
            val orientation =
                typedArray.getInt(R.styleable.DocView_dv_moving_orientation, DocMovingOrientation.HORIZONTAL.orientation)
            mMovingOrientation = DocMovingOrientation.values().first { it.orientation == orientation }
            val ratio =
                typedArray.getInt(R.styleable.DocView_dv_quality, PdfQuality.NORMAL.ratio)
            quality = PdfQuality.values().first { it.ratio == ratio }
            val engineValue =
                typedArray.getInt(R.styleable.DocView_dv_engine, DocEngine.INTERNAL.value)
            engine = DocEngine.values().first { it.value == engineValue }
            showDivider = typedArray.getBoolean(R.styleable.DocView_dv_showDivider, true)
            showPageNum = typedArray.getBoolean(R.styleable.DocView_dv_show_page_num, true)
            divider = typedArray.getDrawable(R.styleable.DocView_dv_divider)
            enableLoadingForPages = typedArray.getBoolean(R.styleable.DocView_dv_enableLoadingForPages, enableLoadingForPages)
            pbHeight = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_pb_height, pbDefaultHeight)
            pbColor = typedArray.getColor(R.styleable.DocView_dv_page_pb_color, pbDefaultColor)

            val marginDim = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_margin, 0)
            pageMargin = Rect(marginDim, marginDim, marginDim, marginDim).apply {
                top = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginTop, top)
                left = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginLeft, left)
                right = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginRight, right)
                bottom = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginBottom, bottom)
            }

            var layoutParams = binding.mPlLoadProgress.layoutParams
            layoutParams.height = pbHeight
            binding.mPlLoadProgress.layoutParams = layoutParams

            binding.mPlLoadProgress.progressTintList = ColorStateList.valueOf(pbColor)
        } catch (_: Throwable) {
        } finally {
            typedArray.recycle()
            runnable = Runnable {
                binding.mPdfPageNo.hide()
            }
        }
    }

    fun setInterceptTouchEventListener(listener: (MotionEvent?) -> Boolean) {
        setInterceptTouchEventListener = listener
    }

    fun removeInterceptTouchEventListener() {
        setInterceptTouchEventListener = null
    }


    fun openDoc(
        activity: Activity, docUrl: String?, docSourceType: Int,
        engine: DocEngine = this.engine
    ) {
        mActivity = activity
        openDoc(activity, docUrl, docSourceType, -1, false, engine)
    }

    fun openDoc(
        activity: Activity?, docUrl: String?,
        docSourceType: Int, fileType: Int,
        viewPdfInPage: Boolean = false,
        engine: DocEngine = this.engine
    ) {
        if (docUrl.isNullOrEmpty()) return
        runCatching {
            var fileType = fileType
            var docUrl = docUrl
            var docSourceType = docSourceType
            if (docUrl != null && docSourceType == DocSourceType.URI && fileType == -1) {
                val uri = try {
                    docUrl.toUri()
                } catch (e: Throwable) {
                    Timber.e(TAG, "parser URI ERROR uri = $docUrl , error : $e")
                    return
                }
                Timber.d("openDoc reset uri = $uri")
                val file = UriUtils.uri2File(uri)
                if (file != null) {
                    var mimeType = ""
                    fileType = FileUtils.getFileTypeForUrl(file.absolutePath)
                    if (fileType == FileType.NOT_SUPPORT) {
                        mimeType = FileUtils.getFileMimeType(context, uri) ?: "*/*"
                        fileType = FileUtils.getFileTypeForUrl(FileUtils.mimeExtMap[mimeType] ?: "")
                    }
                    docUrl = file.absolutePath
                    docSourceType = DocSourceType.PATH
                    Timber.d("openDoc reset url = $docUrl")
                    Timber.d("openDoc reset docSourceType = $docSourceType")
                    Timber.d("openDoc reset fileType = $fileType, mimeType = $mimeType")
                } else {
                    Timber.d("file = null")
                }
            }
            Timber.e("openDoc()......fileType = $fileType")
            mActivity = activity
            mViewPdfInPage = viewPdfInPage
            if (docSourceType == DocSourceType.PATH) {
                sourceFilePath = docUrl
            } else {
                sourceFilePath = null
            }
            if (docSourceType == DocSourceType.URL && fileType != FileType.IMAGE) {
                if (isOpenGoogleBrowser(docUrl.orEmpty())) {
                    showByWeb(docUrl ?: "", DocEngine.GOOGLE)
                    return
                }
                if (engine == DocEngine.MICROSOFT || engine == DocEngine.XDOC || engine == DocEngine.GOOGLE
                ) {
                    showByWeb(docUrl ?: "", engine)
                    return
                }
                downloadFile(docUrl ?: "")
                return
            }

            var type = FileUtils.getFileTypeForUrl(docUrl)
            if (fileType > 0) {
                type = fileType
            }
            when (type) {
                FileType.HTML -> {
                    Timber.e("openDoc()......PDF")
                    binding.mDocWeb.show()
                    binding.mFlDocContainer.hide()
                    binding.mRvPdf.hide()
                    binding.mIvImage.hide()
                    binding.mDocWeb.loadUrl(docUrl.orEmpty())
                }

                FileType.PDF -> {
                    Timber.e("openDoc()......PDF")
                    binding.mDocWeb.hide()
                    binding.mFlDocContainer.hide()
                    binding.mRvPdf.show()
                    binding.mIvImage.hide()

                    showPdf(docSourceType, docUrl)
                }

                FileType.IMAGE -> {
                    if (showPageNum) {
                        showPageNum = false
                    }
                    Timber.e("openDoc()......")
                    binding.mDocWeb.hide()
                    binding.mFlDocContainer.hide()
                    binding.mRvPdf.hide()
                    binding.mIvImage.show()
                    if (docSourceType == DocSourceType.PATH) {
                        Timber.e("openDoc()......PATH")
                        binding.mIvImage.load(File(docUrl.orEmpty()))
                    } else {
                        Timber.e("openDoc()......URL")
                        binding.mIvImage.load(docUrl)
                    }
                }

                FileType.NOT_SUPPORT -> {
                    if (showPageNum) {
                        showPageNum = false
                    }
                    Timber.e("openDoc()......NOT_SUPPORT")
                    binding.mDocWeb.show()
                    binding.mFlDocContainer.hide()
                    binding.mRvPdf.hide()
                    binding.mIvImage.hide()
                    showByWeb(docUrl ?: "", this.engine)
                }

                else -> {
                    Timber.e("openDoc()......ELSE")
                    if (showPageNum) {
                        showPageNum = false
                    }
                    binding.mDocWeb.hide()
                    binding.mFlDocContainer.show()
                    binding.mRvPdf.hide()
                    binding.mIvImage.hide()
                    activity?.let { showDoc(it, binding.mFlDocContainer, docUrl, docSourceType, fileType) }
                }
            }
        }.getOrElse {
            Timber.e(TAG, "load Doc Error : $it")
        }
    }

    fun isOpenGoogleBrowser(url: String): Boolean = FileUtils.getFileTypeForUrl(URL(url).path).let { it == FileType.CSV || it == FileType.RTF }

    fun showDoc(
        activity: Activity,
        mDocContainer: ViewGroup?,
        url: String?,
        docSourceType: Int,
        fileType: Int
    ) {
        Timber.e("showDoc()......")
        var iOffice: IOffice = object : IOffice() {
            override fun getActivity(): Activity {
                return activity
            }

            override fun openFileFinish() {
                mDocContainer?.postDelayed({
                    mDocContainer.removeAllViews()
                    mDocContainer.addView(
                        view,
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }, 200)
            }

            override fun openFileFailed() {
                try {
                    if (mPoiViewer == null) {
                        mPoiViewer = PoiViewer(context)
                    }
                    mPoiViewer?.loadFile(binding.mFlDocContainer, sourceFilePath)
                } catch (e: Exception) {
                    e.printStackTrace()
//                    Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
                }
            }

            override fun getAppName(): String {
                return context.resources.getString(R.string.loading)
            }

            override fun getTemporaryDirectory(): File {
                val file = activity.getExternalFilesDir(null)
                return file ?: activity.filesDir
            }

            override fun fullScreen(fullscreen: Boolean) {
            }

            override fun getMovingOrientation(): Int {
                return mMovingOrientation.orientation
            }

        }
        iOffice.openFile(url, docSourceType, fileType.toString())
    }

    fun showPdf(docSourceType: Int, url: String?) {
        Timber.e("showPdf()......quality = $quality")
        when (docSourceType) {
            DocSourceType.URL -> {
                Timber.e("showPdf()......URL")
                initWithUrl(url = url ?: "", pdfQuality = quality)
            }

            DocSourceType.URI -> {
                Timber.e("showPdf()......URI")
                initWithUri(fileUri = url ?: "", pdfQuality = quality)
            }

            DocSourceType.PATH -> {
                Timber.e("showPdf()......PATH")
                initWithPath(path = url ?: "", pdfQuality = quality)
            }

            DocSourceType.ASSETS -> {
                Timber.e("showPdf()......ASSETS")
                initWithAssets(fileName = url ?: "", pdfQuality = quality)
            }
        }
    }

    private fun showPdf(file: File, pdfQuality: PdfQuality) {
        Timber.e("initView-exists = ${file.exists()}")
        Timber.e("initView-mViewPdfInPage = $mViewPdfInPage")
        pdfRendererCore = PdfRendererCore(context, file, pdfQuality)
        totalPageCount = pdfRendererCore?.getPageCount() ?: 0
        pdfRendererCoreInitialised = true
        binding.mRvPdf.setEnableScale(true)
        if (mViewPdfInPage) {
            binding.mRvPdf.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.mRvPdf.adapter = PdfPageViewAdapter(pdfRendererCore, pageMargin, enableLoadingForPages)
            PagerSnapHelper().attachToRecyclerView(binding.mRvPdf)
        } else {
            binding.mRvPdf.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.mRvPdf.adapter = PdfViewAdapter(pdfRendererCore, pageMargin, enableLoadingForPages, this)
        }
        binding.mRvPdf.itemAnimator = DefaultItemAnimator()
        binding.mRvPdf.addOnScrollListener(scrollListener)

        if (showDivider && !mViewPdfInPage) {
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                divider?.let { setDrawable(it) }
            }.let { binding.mRvPdf.addItemDecoration(it) }
        }
    }

    fun initWithUrl(
        url: String,
        pdfQuality: PdfQuality = this.quality,
        engine: DocEngine = this.engine,
        lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    ) {
        this.lifecycleScope = lifecycleScope
        downloadFile(url, pdfQuality, lifecycleScope)
    }

    fun initWithPath(path: String, pdfQuality: PdfQuality = this.quality) {
        initWithFile(File(path), pdfQuality)
    }

    fun initWithFile(file: File, pdfQuality: PdfQuality = this.quality) {
        showPdf(file, pdfQuality)
    }

    fun initWithAssets(fileName: String, pdfQuality: PdfQuality = this.quality) {
        val file = FileUtils.fileFromAsset(context, fileName)
        showPdf(file, pdfQuality)
    }

    fun initWithUri(fileUri: String, pdfQuality: PdfQuality = this.quality) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            throw UnsupportedOperationException("should be over API 21")

        val file = FileUtils.fileFromUri(context, fileUri)
        showPdf(file, pdfQuality)
    }

    fun downloadFile(
        url: String, pdfQuality: PdfQuality = this.quality,
        lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    ) {
        PdfDownloader(url, this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showByWeb(url: String, engine: DocEngine = this.engine) {
        binding.mDocWeb.mOnWebLoadListener = this

        var engineUrl = "engine"
        when (engine) {
            DocEngine.MICROSOFT -> {
                engineUrl = Constant.MICROSOFT_URL
            }

            DocEngine.XDOC -> {
                engineUrl = Constant.XDOC_VIEW_URL
            }

            DocEngine.GOOGLE -> {
                engineUrl = Constant.GOOGLE_URL
            }

            else -> {
                engineUrl = Constant.XDOC_VIEW_URL
            }
        }
        binding.mDocWeb.loadUrl("$engineUrl${URLEncoder.encode(url, "UTF-8")}")
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return setInterceptTouchEventListener?.invoke(ev) ?: super.onInterceptTouchEvent(ev)
    }

    override fun getDownloadContext() = context

    override fun onDownloadStart() {
        Timber.e("initWithUrl-onDownloadStart()......")
    }

    override fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {
        var progress = (currentBytes.toFloat() / totalBytes.toFloat() * 100F).toInt()
        if (progress >= 100) {
            progress = 100
        }
        Timber.e("initWithUrl-onDownloadProgress()......progress = $progress")
        showLoadingProgress(progress)
    }

    override fun onDownloadSuccess(absolutePath: String) {
        Timber.e("initWithUrl-onDownloadSuccess()......")
        showLoadingProgress(100)
        sourceFilePath = absolutePath
        openDoc(mActivity, absolutePath, DocSourceType.PATH, -1, mViewPdfInPage)
    }

    override fun onError(error: Throwable) {
        error.printStackTrace()
        Timber.e("initWithUrl-onError()......${error.localizedMessage}")
        showLoadingProgress(100)
    }

    override fun getCoroutineScope() = lifecycleScope

    override fun OnWebLoadProgress(progress: Int) {
        showLoadingProgress(progress)
    }

    override fun onTitle(title: String?) {
    }

    fun showLoadingProgress(progress: Int) {
        if (progress == 100) {
            binding.mPlLoadProgress.hide()
        } else {
            binding.mPlLoadProgress.show()
            binding.mPlLoadProgress.progress = progress
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            (recyclerView.layoutManager as LinearLayoutManager).run {
                var foundPosition: Int = findLastCompletelyVisibleItemPosition()

                if (foundPosition != RecyclerView.NO_POSITION) {
                    binding.mPdfPageNo.text = context.getString(R.string.pdfView_page_no, foundPosition + 1, totalPageCount)
                }
                if (showPageNum) {
                    binding.mPdfPageNo.visibility = VISIBLE
                }

                if (foundPosition == 0 && !mViewPdfInPage)
                    binding.mPdfPageNo.postDelayed({
                        binding.mPdfPageNo.visibility = GONE
                    }, 3000)

                if (foundPosition != RecyclerView.NO_POSITION) {
                    mOnDocPageChangeListener?.OnPageChanged(foundPosition, totalPageCount)
                    return@run
                }
                foundPosition = findFirstVisibleItemPosition()
                if (foundPosition != RecyclerView.NO_POSITION) {
                    mOnDocPageChangeListener?.OnPageChanged(foundPosition, totalPageCount)
                    return@run
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE && !mViewPdfInPage) {
                binding.mPdfPageNo.postDelayed(runnable, 3000)
            } else {
                binding.mPdfPageNo.removeCallbacks(runnable)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    fun closePdfRender() {
        try {
            if (pdfRendererCoreInitialised) {
                pdfRendererCore?.closePdfRender()
                pdfRendererCoreInitialised = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun OnPdfItemClick(position: Int) {
        binding.mLlBigPdfImage.show()
        binding.mPbBigLoading.show()
        binding.mIvPdf.setImageBitmap(null)
        pdfRendererCore?.renderPage(position, PdfQuality.ENHANCED) { bitmap: Bitmap?, pageNo: Int ->
            binding.mPbBigLoading.hide()
            binding.mIvPdf.setImageBitmap(bitmap)
            binding.mIvPdf.reset()
            binding.mPdfPageNo.visibility = GONE
        }
    }

    fun onDestroy() {
        mPoiViewer?.recycle()
        closePdfRender()
        mOnDocPageChangeListener = null
    }

    fun deleteFileByPath(): Boolean {
        return runCatching {
            val file = File(sourceFilePath.orEmpty())
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        }.getOrNull() == true
    }
}
