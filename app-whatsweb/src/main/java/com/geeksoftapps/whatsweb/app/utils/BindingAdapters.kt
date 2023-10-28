package com.geeksoftapps.whatsweb.app.utils

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.geeksoftapps.whatsweb.app.R
import java.util.*

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("savedStatusImage")
    fun loadSavedStatusImage(view: ImageView, imagePath: Uri) {
        Glide.with(view.context)
            .load(imagePath)
            .dontAnimate()
            .centerCrop()
            .placeholder(R.drawable.placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(view)
    }
}