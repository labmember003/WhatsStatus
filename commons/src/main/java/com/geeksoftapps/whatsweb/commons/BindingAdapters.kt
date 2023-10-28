package com.geeksoftapps.whatsweb.commons

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("previewImage")
    fun loadPreviewImage(view: ImageView, imagePath: Uri) {
        Glide.with(view.context)
            .load(imagePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }

    @JvmStatic
    @BindingAdapter("previewGif")
    fun loadPreviewGif(view: ImageView, imagePath: Uri) {
        Glide.with(view.context)
            .asGif()
            .load(imagePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}