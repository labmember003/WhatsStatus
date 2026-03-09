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
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.geeksoftapps.whatsweb.commons.R
import com.geeksoftapps.whatsweb.commons.Utils
import com.geeksoftapps.whatsweb.commons.databinding.*
import com.geeksoftapps.whatsweb.commons.toast

class MediaViewPagerAdapter(
    private val context: Context,
    private var mediaFilesList: List<DocumentFile> = listOf()
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var layoutInflater: LayoutInflater? = null

    private val playerList = SparseArray<ExoPlayer>()
    val callback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            pause()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ImageViewHolder(val binding: ListItemViewPagerImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.ivPreview.apply {
                layoutParams = ConstraintLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setOnTouchListener { _, event ->
                    if (event.pointerCount >= 2 || canScrollHorizontally(1) && canScrollHorizontally(-1)) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP -> {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                    false
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = layoutInflater ?: LayoutInflater.from(parent.context)
        return when(MediaType.from(viewType)) {
            MediaType.IMAGE -> ImageViewHolder(ListItemViewPagerImagePreviewBinding.inflate(inflater, parent, false))
            MediaType.VIDEO -> VideoViewHolder(ListItemViewPagerVideoPreviewBinding.inflate(inflater, parent, false))
            MediaType.AUDIO -> AudioViewHolder(ListItemViewPagerAudioPreviewBinding.inflate(inflater, parent, false))
            MediaType.DOCUMENT -> DocumentViewHolder(ListItemViewPagerDocumentPreviewBinding.inflate(inflater, parent, false))
            MediaType.GIF -> GifViewHolder(ListItemViewPagerGifPreviewBinding.inflate(inflater, parent, false))
            MediaType.OTHER -> OtherViewHolder(ListItemViewPagerOtherPreviewBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFilesList[position].mediaType().value
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaFile = mediaFilesList[position]
        val uri = mediaFile.uri
        when(MediaType.from(getItemViewType(position))) {
            MediaType.IMAGE -> {
                (holder as ImageViewHolder).binding.mediaFile = uri
            }
            MediaType.VIDEO -> {
                (holder as VideoViewHolder).let {
                    it.binding.mediaFile = uri
                    var player = playerList[position]
                    if (player == null) {
                        player = ExoPlayer.Builder(context).build()
                        player.setMediaItem(MediaItem.fromUri(uri))
                        player.prepare()
                        player.playWhenReady = false
                        playerList.put(position, player)
                    }
                    it.binding.playerView.player = player
                }
            }
            MediaType.AUDIO -> {
                (holder as AudioViewHolder).let {
                    it.binding.mediaFile = mediaFile
                    var player = playerList[position]
                    if (player == null) {
                        player = ExoPlayer.Builder(context).build()
                        player.setMediaItem(MediaItem.fromUri(uri))
                        player.prepare()
                        player.playWhenReady = false
                        playerList.put(position, player)
                    }
                    it.binding.playerView.player = player
                }
            }
            MediaType.DOCUMENT -> {
                (holder as DocumentViewHolder).let {
                    it.binding.mediaFile = mediaFile
                    it.binding.btnOpen.setOnClickListener {
                        try {
                            context.startActivity(Utils.getViewIntent(context, mediaFile, context.packageName + ".commons_provider"))
                        } catch (e: ActivityNotFoundException) {
                            toast(context.getString(R.string.no_app_present_to_open_this_file))
                        }
                    }
                }
            }
            MediaType.GIF -> {
                (holder as GifViewHolder).binding.mediaFile = uri
            }
            MediaType.OTHER -> {
                (holder as OtherViewHolder).let {
                    it.binding.mediaFile = mediaFile
                    it.binding.btnOpen.setOnClickListener {
                        try {
                            context.startActivity(Utils.getViewIntent(context, mediaFile, context.packageName + ".commons_provider"))
                        } catch (e: ActivityNotFoundException) {
                            toast(context.getString(R.string.no_app_present_to_open_this_file))
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        for (i in 0 until playerList.size()) {
            playerList.valueAt(i)?.playWhenReady = false
        }
    }

    fun release() {
        for (i in 0 until playerList.size()) {
            playerList.valueAt(i)?.release()
        }
        playerList.clear()
    }

    override fun getItemCount(): Int = mediaFilesList.size
}

enum class MediaType(val value: Int) {
    IMAGE(0), VIDEO(1), DOCUMENT(2), AUDIO(3), GIF(4), OTHER(5);
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
