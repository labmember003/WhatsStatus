package com.geeksoftapps.whatsweb.commons.media

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.geeksoftapps.whatsweb.commons.BasicActivity
import com.geeksoftapps.whatsweb.commons.R
import com.geeksoftapps.whatsweb.commons.databinding.ActivityMediaBinding

abstract class MediaActivity : BasicActivity(), OnActionListener {

    private var mediaFileList = listOf<DocumentFile>()

    private lateinit var viewPagerAdapter: MediaViewPagerAdapter

    private var viewPagerCurrentItemSetOnce = false

    private lateinit var binding: ActivityMediaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_media)

        viewPagerAdapter =
            MediaViewPagerAdapter(
                this,
                mediaFileList
            )
        binding.vpMediaPreview.adapter = viewPagerAdapter
        binding.vpMediaPreview.registerOnPageChangeCallback(viewPagerAdapter.callback)

        binding.ivShare.isVisible = showShareButton()
        binding.ivDelete.isVisible = showDeleteButton()
        binding.ivSave.isVisible = showSaveButton()

        binding.ivShare.setOnClickListener { onShareButtonClick(binding.vpMediaPreview.currentItem) }
        binding.ivDelete.setOnClickListener { onDeleteButtonClick(binding.vpMediaPreview.currentItem) }
        binding.ivSave.setOnClickListener { onSaveButtonClick(binding.vpMediaPreview.currentItem) }
    }

    fun getCurrentItem() = getItem(binding.vpMediaPreview.currentItem)

    fun getMediaCount() = viewPagerAdapter.itemCount

    fun getItem(position: Int): DocumentFile? {
        if (position < 0 || position >= mediaFileList.size) return null
        return mediaFileList[position]
    }

    fun setMediaFiles(mediaFileList: List<DocumentFile>, selectedIndex: Int? = null,
                      setSelectedOnce: Boolean = false) {
        this.mediaFileList = mediaFileList
        viewPagerAdapter.setMediaFilesList(mediaFileList)
        selectedIndex?.let {
            if (!setSelectedOnce ||!viewPagerCurrentItemSetOnce) {
                binding.vpMediaPreview.setCurrentItem(selectedIndex, false)
                viewPagerCurrentItemSetOnce = true
            }
        }
    }

    override fun onDestroy() {
        viewPagerAdapter.release()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        viewPagerAdapter.pause()
    }

    abstract fun showDeleteButton(): Boolean

    abstract fun showSaveButton(): Boolean

    abstract fun showShareButton(): Boolean
}

interface OnActionListener {
    fun onSaveButtonClick(position: Int)

    fun onDeleteButtonClick(position: Int)

    fun onShareButtonClick(position: Int)
}