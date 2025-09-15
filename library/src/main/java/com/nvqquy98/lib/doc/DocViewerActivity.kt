package com.nvqquy98.lib.doc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nvqquy98.lib.doc.bean.DocEngine
import com.nvqquy98.lib.doc.databinding.ActivityDocViewerBinding
import com.nvqquy98.lib.doc.util.Constant

import timber.log.Timber

open class DocViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocViewerBinding

    companion object {
        fun launchDocViewer(
            activity: AppCompatActivity, docSourceType: Int, path: String?,
            fileType: Int? = null, engine: Int? = null
        ) {
            var intent = Intent(activity, DocViewerActivity::class.java)
            intent.putExtra(Constant.INTENT_SOURCE_KEY, docSourceType)
            intent.putExtra(Constant.INTENT_DATA_KEY, path)
            intent.putExtra(Constant.INTENT_TYPE_KEY, fileType)
            intent.putExtra(Constant.INTENT_ENGINE_KEY, engine)
            activity.startActivity(intent)
        }
    }

    var docSourceType = 0
    var fileType = -1
    var engine: Int = DocEngine.INTERNAL.value
    var docUrl: String? = null// 文件地址

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData(intent)
    }

    fun initData(intent: Intent?) {
        docUrl = intent?.getStringExtra(Constant.INTENT_DATA_KEY)
        docSourceType = intent?.getIntExtra(Constant.INTENT_SOURCE_KEY, 0) ?: 0
        fileType = intent?.getIntExtra(Constant.INTENT_TYPE_KEY, -1) ?: -1
        engine = intent?.getIntExtra(Constant.INTENT_ENGINE_KEY, DocEngine.INTERNAL.value) ?: DocEngine.INTERNAL.value
        binding.mDocView.openDoc(this, docUrl, docSourceType, fileType, false, DocEngine.values().first { it.value == engine })
        Timber.e("initData-docUrl = $docUrl")
        Timber.e("initData-docSourceType = $docSourceType")
        Timber.e("initData-fileType = $fileType")
        Timber.e("initData-engine = $engine")
    }
}
