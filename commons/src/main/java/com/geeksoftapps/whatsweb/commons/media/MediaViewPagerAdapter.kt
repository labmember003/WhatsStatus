package com.geeksoftapps.whatsweb.commons.media

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.forEach
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.geeksoftapps.whatsweb.commons.R
import com.geeksoftapps.whatsweb.commons.Utils
import com.geeksoftapps.whatsweb.commons.databinding.*
import com.geeksoftapps.whatsweb.commons.toast

class MediaViewPagerAdapter(
    private val context: Context,
    private var mediaFilesList: List<DocumentFile> = listOf()
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var layoutInflater: LayoutInflater? = null

    private val playerList = SparseArray<SimpleExoPlayer>()
    val callback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            playerList.forEach { key, player ->
                player.playWhenReady = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ImageViewHolder(val binding: ListItemViewPagerImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.ivPreview.apply {
                layoutParams = ConstraintLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setOnTouchListener { view, event ->
                    var result = true
                    //can scroll horizontally checks if there's still a part of the image
                    //that can be scrolled until you reach the edge
                    if (event.pointerCount >= 2 || view.canScrollHorizontally(1) && canScrollHorizontally(-1)) {
                        //multi-touch event
                        result = when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                parent.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                            MotionEvent.ACTION_UP -> {
                                parent.requestDisallowInterceptTouchEvent(false)
                                true
                            }
                            else -> true
                        }
                    }
                    result
                }
            }
        }
    }

    inner class VideoViewHolder(val binding: ListItemViewPagerVideoPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class AudioViewHolder(val binding: ListItemViewPagerAudioPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class DocumentViewHolder(val binding: ListItemViewPagerDocumentPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class GifViewHolder(val binding: ListItemViewPagerGifPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class OtherViewHolder(val binding: ListItemViewPagerOtherPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setMediaFilesList(mediaFilesList: List<DocumentFile>) {
        this.mediaFilesList = mediaFilesList
        release()
        playerList.clear()
        notifyDataSetChanged()
    }

    fun getMediaFilesList(): List<DocumentFile> {
        return mediaFilesList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val mediaType =
            MediaType.from(
                viewType
            )
        return when(mediaType) {
            MediaType.IMAGE -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerImagePreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_image_preview,
                    parent,
                    false
                )
                ImageViewHolder(binding)
            }
            MediaType.VIDEO -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerVideoPreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_video_preview,
                    parent,
                    false
                )
                VideoViewHolder(binding)
            }
            MediaType.AUDIO -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerAudioPreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_audio_preview,
                    parent,
                    false
                )
                AudioViewHolder(binding)
            }
            MediaType.DOCUMENT -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerDocumentPreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_document_preview,
                    parent,
                    false
                )
                DocumentViewHolder(binding)
            }
            MediaType.GIF -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerGifPreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_gif_preview,
                    parent,
                    false
                )
                GifViewHolder(binding)
            }
            MediaType.OTHER -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerOtherPreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_other_preview,
                    parent,
                    false
                )
                OtherViewHolder(binding)
            }
            else -> {
                val binding = DataBindingUtil.inflate<ListItemViewPagerImagePreviewBinding>(
                    layoutInflater?: LayoutInflater.from(parent.context),
                    R.layout.list_item_view_pager_image_preview,
                    parent,
                    false
                )
                ImageViewHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFilesList[position].mediaType().value
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(MediaType.from(
            getItemViewType(position)
        )) {
            MediaType.IMAGE -> {
                (holder as ImageViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position].uri
                }
            }
            MediaType.VIDEO -> {
                (holder as VideoViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position].uri
                    var player = playerList[position]
                    if (player == null) {
                        player = SimpleExoPlayer.Builder(context).build()
                        val dataSourceFactory = DefaultDataSourceFactory(context,
                            Util.getUserAgent(context, "auto_rdm"))
                        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaFilesList[position].uri)
                        player.prepare(videoSource)
                        player.playWhenReady = false
                        playerList[position] = player
                    }
                    holder.binding.playerView.player = player
                }
            }
            MediaType.AUDIO -> {
                (holder as AudioViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position]
                    var player = playerList[position]
                    if (player == null) {
                        player = SimpleExoPlayer.Builder(context).build()
                        val dataSourceFactory = DefaultDataSourceFactory(context,
                            Util.getUserAgent(context, "auto_rdm"))
                        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaFilesList[position].uri)
                        player.prepare(audioSource)
                        player.playWhenReady = false
                        playerList[position] = player
                    }
                    holder.binding.playerView.player = player
                }
            }
            MediaType.DOCUMENT -> {
                (holder as DocumentViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position]
                    holder.binding.btnOpen.setOnClickListener {
                        try {
                            context.startActivity(Utils.getViewIntent(context,
                                mediaFilesList[position],
                                context.applicationContext.packageName + ".commons_provider"
                            ))
                        } catch (e: ActivityNotFoundException) {
                            toast(context.getString(R.string.no_app_present_to_open_this_file))
                        }

                    }
                }
            }
            MediaType.GIF -> {
                (holder as GifViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position].uri
                }
            }
            MediaType.OTHER -> {
                (holder as OtherViewHolder).let {
                    holder.binding.mediaFile = mediaFilesList[position]
                    holder.binding.btnOpen.setOnClickListener {
                        try {
                            context.startActivity(Utils.getViewIntent(context,
                                mediaFilesList[position],
                                context.applicationContext.packageName + ".commons_provider"
                            ))
                        } catch (e: ActivityNotFoundException) {
                            toast(context.getString(R.string.no_app_present_to_open_this_file))
                        }

                    }
                }
            }
        }
    }

    fun pause() {
        playerList.forEach { key, player ->
            player.playWhenReady = false
        }
    }

    fun release() {
        playerList.forEach { key, player ->
            player.release()
        }
    }

    override fun getItemCount(): Int = mediaFilesList.size
}

enum class MediaType(val value: Int) {
    IMAGE(0),
    VIDEO(1),
    DOCUMENT(2),
    AUDIO(3),
    GIF(4),
    OTHER(5);

    companion object {
        fun from(findValue: Int) = values().first { it.value == findValue }
    }
}

fun DocumentFile.mediaType(): MediaType {
    return when {
        this.name?.endsWith(".gif", true) == true -> MediaType.GIF
        this.type?.contains("image") == true -> MediaType.IMAGE
        this.type?.contains("video") == true -> MediaType.VIDEO
        this.type?.contains("audio") == true -> MediaType.AUDIO
        else -> MediaType.DOCUMENT
    }
}