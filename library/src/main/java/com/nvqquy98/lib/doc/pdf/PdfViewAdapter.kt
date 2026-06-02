package com.nvqquy98.lib.doc.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.nvqquy98.lib.doc.databinding.ListItemPdfBinding
import com.nvqquy98.lib.doc.interfaces.OnPdfItemClickListener
import com.nvqquy98.lib.doc.util.ViewUtils.hide
import com.nvqquy98.lib.doc.util.ViewUtils.show
import androidx.core.graphics.createBitmap

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: PdfViewAdapter
 * Author: Victor
 * Date: 2023/09/28 11:17
 * Description: 
 * -----------------------------------------------------------------
 */

internal class PdfViewAdapter(
    private val renderer: PdfRendererCore?,
    private val pageSpacing: Rect,
    private val enableLoadingForPages: Boolean,
    private val listener: OnPdfItemClickListener?
) : RecyclerView.Adapter<PdfViewAdapter.PdfPageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        return PdfPageViewHolder(ListItemPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        try {
            return renderer?.getPageCount() ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bindView()
    }

    inner class PdfPageViewHolder(private val binding: ListItemPdfBinding) : RecyclerView.ViewHolder(binding.root), View.OnAttachStateChangeListener {
        fun bindView() {
        }

        init {
            binding.containerView.setOnClickListener {
                listener?.OnPdfItemClick(bindingAdapterPosition)
            }
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

        init {
            binding.root.addOnAttachStateChangeListener(this)
        }

        override fun onViewAttachedToWindow(p0: View) {
            handleLoadingForPage(bindingAdapterPosition)
            renderer?.renderPage(bindingAdapterPosition) { bitmap: Bitmap?, pageNo: Int ->
                if (pageNo == bindingAdapterPosition) {
                    bitmap?.let {
                        binding.containerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            height =
                                (binding.containerView.width.toFloat() / ((bitmap.width.toFloat() / bitmap.height.toFloat()))).toInt()
                            this.topMargin = pageSpacing.top
                            this.leftMargin = pageSpacing.left
                            this.rightMargin = pageSpacing.right
                            this.bottomMargin = pageSpacing.bottom
                        }
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

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val bitmap = createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)

        //canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }
}
