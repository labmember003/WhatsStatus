package com.geeksoftapps.whatsweb.app.ui.status.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.databinding.ListItemSavedStatusBinding

class StatusAdapter(private val context: Context) : RecyclerView.Adapter<StatusAdapter.ViewHolder>() {

    private var statusFiles = listOf<DocumentFile>()
    private var eventListener: EventListener? = null

    fun setStatusFilesList(statusFiles: List<DocumentFile>) {
        this.statusFiles = statusFiles
        notifyDataSetChanged()
    }

    fun setEventListener(eventListener: EventListener) {
        this.eventListener = eventListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ListItemSavedStatusBinding>(
            LayoutInflater.from(context),
            R.layout.list_item_saved_status, parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val statusFile = statusFiles[position]
        holder.binding.root.setOnClickListener { eventListener?.onStatusItemClick(statusFile) }
    }

    override fun getItemCount() = statusFiles.size

    class ViewHolder(val binding: ListItemSavedStatusBinding) : RecyclerView.ViewHolder(binding.root)

    interface EventListener {
        fun onStatusItemClick(statusFile: DocumentFile)
    }

    /** Helper used by SavedStatusAdapter to track per-item aspect ratios */
    data class ConstraintSetRatioContainer(
        var ratio: String = "1:1",
        var isSet: Boolean = false
    )
}

