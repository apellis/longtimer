package com.ape.longtimer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ape.longtimer.R
import com.ape.longtimer.Utils
import com.ape.longtimer.model.Segment

class SegmentItemAdapter(
    private val context: Context,
    private val dataset: List<Segment>
) : RecyclerView.Adapter<SegmentItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.segment_list_item, parent, false)

        return ItemViewHolder(adapterLayout)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.textView.text = context.getString(
            R.string.segment_string, item.name,
            Utils.timeStringFromMillis(item.durationSecs.toLong() * 1000))
    }
}