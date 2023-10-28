package com.geeksoftapps.whatsweb.app.ui.status.adapters

import android.content.Context
import androidx.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.databinding.ListItemSavedStatusBinding
import com.geeksoftapps.whatsweb.status.StatusType
import com.geeksoftapps.whatsweb.status.statusType
import kotlin.random.Random


class StatusAdapter(val context: Context): RecyclerView.Adapter<StatusAdapter.InnerViewHolder>() {

    private var layoutInflater: LayoutInflater? = null
    private var statusFilesList: List<DocumentFile> = listOf()
    private var eventListener: EventListener? = null

    data class ConstraintSetRatioContainer(
            var isSet: Boolean = false,
            var ratio: String? = null
    )

    private var ratioList = emptyList<ConstraintSetRatioContainer>()
    private val set = ConstraintSet()

    inner class InnerViewHolder(val binding: ListItemSavedStatusBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setStatusFilesList(statusFilesList:List<DocumentFile>){
        this.statusFilesList = statusFilesList
        this.ratioList = List(statusFilesList.size) { ConstraintSetRatioContainer() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val binding = DataBindingUtil.inflate<ListItemSavedStatusBinding>(
            layoutInflater?: LayoutInflater.from(parent.context),
            R.layout.list_item_saved_status,
            parent,
            false
        )
        return InnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        holder.binding.savedStatusDocumentFileUri = statusFilesList[position].uri
        holder.binding.ivPlay.visibility =
            if (statusFilesList[position].statusType() == StatusType.VIDEO) View.VISIBLE
            else View.GONE

        if (!ratioList[position].isSet) {
            val ratio = String.format("%d:%d", 30, Random.nextInt(30, 50))
            ratioList[position].ratio = ratio
            ratioList[position].isSet = true
        }
        val ratio = String.format("%d:%d", 40, 40)
        set.clone(holder.binding.mConstraintLayout)
        set.setDimensionRatio(holder.binding.ivSavedStatus.id, ratio)
        set.applyTo(holder.binding.mConstraintLayout)

        holder.binding.ivSavedStatus.setOnClickListener {
            eventListener?.onStatusItemClick(statusFilesList[position])
        }
    }

    fun setEventListener(eventListener: EventListener) {
        this.eventListener = eventListener
    }

    override fun getItemCount(): Int {
        return statusFilesList.size
    }

    interface EventListener {
        fun onStatusItemClick(statusFile: DocumentFile)
    }
}