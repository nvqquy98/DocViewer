package com.cherry.doc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.cherry.doc.R
import com.cherry.doc.databinding.ActivityWordBinding
import com.nvqquy98.lib.doc.util.Constant
import com.nvqquy98.lib.doc.widget.PoiViewer

class WordActivity : AppCompatActivity() {
    companion object {
        fun launchDocViewer(activity: AppCompatActivity, path: String?) {
            var intent = Intent(activity, WordActivity::class.java)
            intent.putExtra(Constant.INTENT_DATA_KEY, path)
            activity.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityWordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var path = intent?.getStringExtra(Constant.INTENT_DATA_KEY)
        word2Html(path ?: "")
    }

    fun word2Html(sourceFilePath: String) {
        var mPoiViewer = PoiViewer(this)
        try {
            mPoiViewer?.loadFile(binding.mFlDocContainer, sourceFilePath)
        } catch (e: java.lang.Exception) {
//            Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show()
        }
    }
}
