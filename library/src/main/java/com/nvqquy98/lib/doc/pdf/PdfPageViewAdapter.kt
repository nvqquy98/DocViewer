package com.nvqquy98.lib.doc.pdf

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.nvqquy98.lib.doc.databinding.PageItemPdfBinding
import com.nvqquy98.lib.doc.util.ViewUtils.hide
import com.nvqquy98.lib.doc.util.ViewUtils.show

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: PdfPageViewAdapter
 * Author: Victor
 * Date: 2023/09/28 11:17
 * Description: 
 * -----------------------------------------------------------------
 */

internal class PdfPageViewAdapter(
    private val renderer: PdfRendererCore?,
    private val pageSpacing: Rect,
    private val enableLoadingForPages: Boolean
) : RecyclerView.Adapter<PdfPageViewAdapter.PdfPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        return PdfPageViewHolder(PageItemPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return renderer?.getPageCount() ?: 0
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bindView()
    }

    inner class PdfPageViewHolder(private val binding: PageItemPdfBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnAttachStateChangeListener {

        init {
            binding.root.addOnAttachStateChangeListener(this)
        }

        fun bindView() {
        }

        private fun handleLoadingForPage(position: Int) {
            if (!enableLoadingForPages) {
                binding.loadingView.root.hide()
                return
            }

            if (renderer?.pageExistInCache(position) == true) {
                binding.loadingView.root.hide()
            } else {
                binding.loadingView.root.show()
            }
        }

        override fun onViewAttachedToWindow(p0: View) {
            handleLoadingForPage(bindingAdapterPosition)
            renderer?.renderPage(bindingAdapterPosition) { bitmap: Bitmap?, pageNo: Int ->
                if (pageNo == bindingAdapterPosition) {
                    bitmap?.let {
//                        itemView.container_view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//                            height =
//                                (itemView.container_view.width.toFloat() / ((bitmap.width.toFloat() / bitmap.height.toFloat()))).toInt()
//                            this.topMargin = pageSpacing.top
//                            this.leftMargin = pageSpacing.left
//                            this.rightMargin = pageSpacing.right
//                            this.bottomMargin = pageSpacing.bottom
//                        }
                        binding.pageView.setImageBitmap(bitmap)
                        binding.pageView.animation = AlphaAnimation(0F, 1F).apply {
                            interpolator = LinearInterpolator()
                            duration = 200
                        }
                        binding.loadingView.root.hide()
                    }
                }
            }
        }

        override fun onViewDetachedFromWindow(p0: View) {
            binding.pageView.setImageBitmap(null)
            binding.pageView.clearAnimation()
        }
    }
}
