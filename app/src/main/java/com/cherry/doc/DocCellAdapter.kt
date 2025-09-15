package com.cherry.doc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.DocInfo
import com.cherry.doc.R
import com.cherry.doc.databinding.RvDocItemCellBinding

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: DocAdapter
 * Author: Victor
 * Date: 2023/10/26 10:56
 * Description: 
 * -----------------------------------------------------------------
 */

class DocCellAdapter(
    var context: Context,
    var listener: AdapterView.OnItemClickListener?,
    var parentPosition: Int
) : RecyclerView.Adapter<DocCellViewHolder>() {

    var datas = ArrayList<DocInfo>()

    fun showDatas(docList: ArrayList<DocInfo>?) {
        datas.clear()
        docList?.let { datas.addAll(it) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocCellViewHolder {
        return DocCellViewHolder(RvDocItemCellBinding.inflate(LayoutInflater.from(context), parent, false), parentPosition)
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun onBindViewHolder(holder: DocCellViewHolder, position: Int) {
        holder.mOnItemClickListener = listener
        holder.bindData(datas[position])
    }
}
