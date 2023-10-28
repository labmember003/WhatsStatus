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


class SavedStatusAdapter(val context: Context?): RecyclerView.Adapter<SavedStatusAdapter.InnerViewHolder>() {

    private var layoutInflater: LayoutInflater? = null
    private  var savedStatusFilesList: List<DocumentFile> = listOf()
    private var eventListener: EventListener? = null

    private var ratioList = emptyList<StatusAdapter.ConstraintSetRatioContainer>()
    private val set = ConstraintSet()

    inner class InnerViewHolder(val binding: ListItemSavedStatusBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setStatusFilesList(statusFilesList:List<DocumentFile>){
        this.savedStatusFilesList = statusFilesList
        this.ratioList = List(statusFilesList.size) { StatusAdapter.ConstraintSetRatioContainer() }
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
        holder.binding.savedStatusDocumentFileUri = savedStatusFilesList[position].uri
        holder.binding.ivPlay.visibility =
            if (savedStatusFilesList[position].statusType() == StatusType.VIDEO) View.VISIBLE
            else View.GONE

        if (!ratioList[position].isSet) {
            val ratio = String.format("%d:%d", 30, Random.nextInt(30, 50))
            ratioList[position].ratio = ratio
            ratioList[position].isSet = true
        }
        val ratio = String.format("%d:%d", 30, 30)
        set.clone(holder.binding.mConstraintLayout)
        set.setDimensionRatio(holder.binding.ivSavedStatus.id, ratio)
        set.applyTo(holder.binding.mConstraintLayout)

        holder.binding.ivSavedStatus.setOnClickListener {
            eventListener?.onSavedStatusItemClick(savedStatusFilesList[position])
        }
    }

    fun setEventListener(eventListener: EventListener) {
        this.eventListener = eventListener
    }

    override fun getItemCount(): Int {
        return savedStatusFilesList.size
    }

    interface EventListener {
        fun onSavedStatusItemClick(statusFile: DocumentFile)
    }
}